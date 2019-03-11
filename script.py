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

        self.ser = serial.Serial("/dev/serial0", 9600)

        self.video_proc = None
        self.video_cmd = "raspivid -n -ih -t 0 -rot 0 -w 1280 -h 720 -fps 25 -b 1000000 -o - | nc -lkv4 8000"

        self.S = 0x33
        self.E = 0x77
        self.cmd_halt = 0x01
        self.cmd_reboot = 0x02
        self.cmd_go = 0x03
        self.cmd_stop = 0x04
        self.cmd_respond = 0x05
        self.cmd_led = 0x06
        self.cmd_video_on = 0x07
        self.cmd_video_off = 0x08

        self.joystick_X = 64
        self.joystick_Y = 64
        self.command = 0

        self.byteCounter = 0

        self.clearSerial()

        self.targetSteps = multiprocessing.Value('i', 64)
        self.stop_stepper = multiprocessing.Value('i', 0)
        
        self.stepper_process = multiprocessing.Process(target = self.run_stepper, args=(self.targetSteps, self.stop_stepper))
        self.stepper_process.start()

        self.writeSerialMessage("PROGRAM STARTED")
        
        try:
            while True:
                self.readSerial()
        except KeyboardInterrupt:
            pass
            self.cleanup()        

        

    def run_stepper(self, target, stop):

        GPIO.setmode(GPIO.BCM)
        stepper_control_pins = [6,13,19,26]

        for pin in stepper_control_pins:
          GPIO.setup(pin, GPIO.OUT)
          GPIO.output(pin, 0)

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

        while stop.value == 0:
            while currentSteps == target.value and stop.value == 0:
                continue

            if stop.value == 1:
                break

            for j in range(8):

                index = j
                if currentSteps > target.value:
                    index = 7 - j
                
                for pin in range(4):
                    GPIO.output(stepper_control_pins[pin], stepper_fwd_seq[index][pin])
                    time.sleep(0.00025)

            if currentSteps > target.value:
                currentSteps -= 1
            else:
                currentSteps += 1
                
        GPIO.cleanup()

            

    def readSerial(self):
        
        while self.ser.in_waiting > 0:
            val = ord(self.ser.read())

            if self.byteCounter == 0:
                
                if val == self.S:           # Start of frame
                
                    self.byteCounter += 1
                
            elif self.byteCounter == 1:     # first byte - joystick X
                
                self.joystick_X = val
                self.byteCounter += 1
                
            elif self.byteCounter == 2:     # second byte - joystick Y
                
                self.joystick_Y = val
                self.byteCounter += 1
                
            elif self.byteCounter == 3:     # third byte - command
                
                self.command = val
                self.byteCounter += 1
                
            elif self.byteCounter == 4:

                if val == self.E:           # End of frame
                    self.newFrameReceived()
                self.byteCounter = 0


                    
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


    def stop(self):
        self.writeSerialMessage("Command: stop")


    def respond(self):
        self.writeSerialMessage("Command: respond")


    def led(self):
        self.writeSerialMessage("Command: led")

        
    def joystickX(self):
        self.targetSteps.value = self.joystick_X

        
    def joystickY(self):
        return


    def video(self, on):
        if on:
            self.writeSerialMessage("Command: video ON")
            if self.video_proc is None:
                self.video_proc = subprocess.Popen(self.video_cmd, stdout=subprocess.PIPE, shell=True, preexec_fn=os.setsid) 
        else:
            self.writeSerialMessage("Command: video OFF")
            if self.video_proc is not None:
                os.killpg(os.getpgid(self.video_proc.pid), signal.SIGTERM)
                self.video_proc = None

            
    def writeSerialMessage(self, message):
        self.ser.write("RPi: " + message + "\r\n")
        self.ser.flush()


    def clearSerial(self):
        while(self.ser.in_waiting > 0):
            self.ser.read()


    def cleanup(self):
        print "cleanup"
        self.video(False)
        self.stepper_process.join()
        self.stop_stepper.value = 1
        #GPIO.cleanup()
        

EGD()
