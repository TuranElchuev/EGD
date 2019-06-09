#!/usr/bin/env python

import serial
import RPi.GPIO as GPIO
import os
import signal
import subprocess
import multiprocessing
import time



class EGD:

    def __init__(self):

        # pins (BCM)
        self.buzzer_pin = 20
        self.led_pin = 21
        self.stepper_pins = [6,13,19,26] # IN1, IN2, IN3, IN4
                        
        GPIO.setmode(GPIO.BCM)

        # configure Push-button
        self.btn_debounce_duration = 0.5 # seconds to wait until next button press can be processed
        self.btn_press_time = 0 # will store time in seconds when button is pressed
        self.btn_pin = 16
        GPIO.setup(self.btn_pin, GPIO.IN, pull_up_down = GPIO.PUD_UP)
        GPIO.add_event_detect(self.btn_pin, GPIO.FALLING, self.btn_pressed)

        # configure LED pin
        self.led_on = False
        GPIO.setup(self.led_pin, GPIO.OUT)
        GPIO.output(self.led_pin, self.led_on)                

        # configure Buzzer pin
        GPIO.setup(self.buzzer_pin, GPIO.OUT)
        GPIO.output(self.buzzer_pin, 0)

        # configure Stepper pins
        for pin in self.stepper_pins:
          GPIO.setup(pin, GPIO.OUT)
          GPIO.output(pin, 0)

        # video transmitting process and command
        self.video_proc = None
        self.video_cmd = "raspivid -n -ih -t 0 -rot 0 -w 1280 -h 720 -fps 25 -b 1000000 -o - | nc -lkv4 8000"

        # Serial frame bytes
        self.S = 0x33                       # Start of frame
        self.E = 0x77                       # End of frame
        self.cmd_halt = 0x01                # command HALT
        self.cmd_reboot = 0x02              # command REBOOT
        self.cmd_go = 0x03                  # command GO
        self.cmd_stop = 0x04                # command STOP
        self.cmd_respond = 0x05             # command RESPOND
        self.cmd_led = 0x06                 # command TOGGLE LED
        self.cmd_video_on = 0x07            # command VIDEO TRANSMISSION ON
        self.cmd_video_off = 0x08           # command VIDEO TRANSMISSION OFF

        self.byteCounter = 0                # Byte counter is used to determine position (purpose) of the received byte

        # last received (current) commands and joystick values
        self.joystick_X = 64
        self.joystick_Y = 64
        self.command = 0
        
        # initialize UART communication (Bluetooth)
        self.ser = serial.Serial("/dev/serial0", 9600)  
        self.clearSerial()

        # Init and start Stepper process
        self.targetSteps = multiprocessing.Value('i', 64) # Stepper will rotate until this value is reached
        self.stop_stepper = multiprocessing.Value('i', 0) # used to stop Stepper process
        
        self.stepper_process = multiprocessing.Process(target = self.run_stepper, args=(self.targetSteps, self.stop_stepper))
        self.stepper_process.start()

        self.writeSerialMessage("PROGRAM STARTED")

        # read UART continuously        
        try:
            while True:
                self.readSerial()
        except KeyboardInterrupt:
            pass
            self.cleanup()        

        
    # Function that is called inside the Stepper process
    def run_stepper(self, target, stop):

        '''
            Sequence of input values of Stepper.
            When applied top-down - one direction,
            bottom-up - another direction.
        '''
        stepper_fwd_seq = [
          [1,0,0,0],
          [1,1,0,0],
          [0,1,0,0],
          [0,1,1,0],
          [0,0,1,0],
          [0,0,1,1],
          [0,0,0,1],
          [1,0,0,1]
        ]

        currentSteps = target.value

        # run infinite loop until stop.value is set 1 (process is stopped)
        while stop.value == 0:

            # wait as long as target step is reached
            while currentSteps == target.value and stop.value == 0:
                continue

            if stop.value == 1:
                break

            # apply stepper input values sequence to approach target steps by 1 step
            for j in range(8):

                index = j # index used to iterate through stepper input values (0...7)

                # if current steps are less than target, then bottom-up iteration
                if currentSteps < target.value:
                    index = 7 - j # swapped indexes will run in the range 7...0

                # apply stepper iput values corresponding to current index
                for pin in range(4):
                    GPIO.output(self.stepper_pins[pin], stepper_fwd_seq[index][pin])
                    time.sleep(0.00025) # delay to ensure sufficient time for rotation (also can be used to adjust speed)

            # increment or decrement current steps to approach target step by 1
            if currentSteps > target.value:
                currentSteps -= 1
            else:
                currentSteps += 1

            
    '''
        Reads UART and samples frames.
        Frame format should be:
        1st byte        self.S (start of frame)
        2nd byte        joystick X value
        3rd byte        joystick Y value
        4th byte        command
        5th byte        self.E (end of frame)
        bytes should have signed positive values (0...127)
    '''
    def readSerial(self):
        
        while self.ser.in_waiting > 0:
            
            val = ord(self.ser.read()) # char to int

            if self.byteCounter == 0:
                
                if val == self.S:           # Start of frame
                
                    self.byteCounter += 1
                
            elif self.byteCounter == 1:     # joystick X
                
                self.joystick_X = val
                self.byteCounter += 1
                
            elif self.byteCounter == 2:     # joystick Y
                
                self.joystick_Y = val
                self.byteCounter += 1
                
            elif self.byteCounter == 3:     # command
                
                self.command = val
                self.byteCounter += 1
                
            elif self.byteCounter == 4:

                if val == self.E:           # End of frame

                    self.newFrameReceived()

                # reset byte counter when the 5th byte (starting from start of the frame) is read
                self.byteCounter = 0

    '''
        Is called when new frame is sampled.
        At this point, self.command, self.joystick_X and self.joystick_Y
        will hold most recent values.
        According to the received command, corresponding function will be called.
    '''
    def newFrameReceived(self):
        
        if self.command == self.cmd_halt:
            self.halt()
        elif self.command == self.cmd_reboot:
            self.reboot()
        elif self.command == self.cmd_go:
            self.go()
        elif self.command == self.cmd_stop:
            self.stop()
        elif self.command == self.cmd_respond:
            self.respond()
        elif self.command == self.cmd_led:
            self.led()
        elif self.command == self.cmd_video_on:
            self.video(True)
        elif self.command == self.cmd_video_off:
            self.video(False)
                
        self.joystickX()

        self.joystickY()


    def halt(self):        
        self.writeSerialMessage("Halt...")
        subprocess.call("sudo halt", shell=True)

        
    def reboot(self):        
        self.writeSerialMessage("Reboot...")
        subprocess.call("sudo reboot", shell=True)


    def go(self):        
        self.writeSerialMessage("Command: go")
        self.buzz(2)


    def stop(self):        
        self.writeSerialMessage("Command: stop")
        self.buzz(3)


    def respond(self):        
        self.writeSerialMessage("Command: respond")
        self.buzz(1)


    def led(self):        
        self.writeSerialMessage("Command: led")
        self.led_on = not self.led_on
        GPIO.output(self.led_pin, self.led_on)        

        
    def joystickX(self):        
        self.targetSteps.value = self.joystick_X

        
    def joystickY(self):        
        return # not used at this stage of the project (only 1 motor, controlled by joystick X)


    def video(self, on):
        
        if on:
            
            self.writeSerialMessage("Command: video ON")
            # create a new video streaming process if no process is running
            if self.video_proc is None:
                self.video_proc = subprocess.Popen(self.video_cmd, stdout=subprocess.PIPE, shell=True, preexec_fn=os.setsid) 
        else:
            
            self.writeSerialMessage("Command: video OFF")
            # kill the video streaming process if exists
            if self.video_proc is not None:
                os.killpg(os.getpgid(self.video_proc.pid), signal.SIGTERM)
                self.video_proc = None


    def buzz(self, beeps):
        # create a separate process to run buzzer
        buzzer_process = multiprocessing.Process(target = self.run_buzzer, args=(beeps, ))
        buzzer_process.start()


    # function that is called inside buzzer process
    def run_buzzer(self, beeps):        
        for i in range(beeps):
            GPIO.output(self.buzzer_pin, 1)
            time.sleep(0.15)
            GPIO.output(self.buzzer_pin, 0)
            time.sleep(0.05)

    # function that is called when button is pressed
    def btn_pressed(self, pin):
        event_time = time.time()
        if(event_time - self.btn_press_time > self.btn_debounce_duration):
            self.btn_press_time = event_time
            # send corresponding string message via serial interface
            self.writeSerialMessage("cmd:btn")


    # sends a string via UART to Android
    def writeSerialMessage(self, message):
        self.ser.write("RPi: " + message + "\r\n")
        self.ser.flush()


    # flushes serial (UART) input buffer
    def clearSerial(self):
        while(self.ser.in_waiting > 0):
            self.ser.read()


    def cleanup(self):
        print "cleanup"
        self.video(False)
        self.stepper_process.join()
        self.stop_stepper.value = 1
        GPIO.cleanup()
        

EGD()
