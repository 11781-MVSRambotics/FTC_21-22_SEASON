package org.firstinspires.ftc.teamcode.opModes;

import static java.lang.Math.abs;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.teamcode.res.ArmPositions;
import org.firstinspires.ftc.teamcode.res.ClawPositions;

import java.util.List;

@Autonomous(name="RedDepot", group="opModes")

public class RedDepot extends LinearOpMode {

    private static final String TFOD_MODEL_ASSET = "BlooBoi_Proto.tflite";
    private static final String[] LABELS = {"BlooBoi"};

    private static final String VUFORIA_KEY =
            "AbskhHb/////AAABmb8nKWBiYUJ9oEFmxQL9H2kC6M9FzPa1acXUaS/H5wRkeNbpNVBJjDfcrhlTV2SIGc/lxBOtq9X7doE2acyeVOPg4sP69PQQmDVQH5h62IwL8x7BS/udilLU7MyX3KEoaFN+eR1o4FKBspsYrIXA/Oth+TUyrXuAcc6bKSSblICUpDXCeUbj17KrhghgcgxU6wzl84lCDoz6IJ9egO+CG4HlsBhC/YAo0zzi82/BIUMjBLgFMc63fc6eGTGiqjCfrQPtRWHdj2sXHtsjZr9/BpLDvFwFK36vSYkRoSZCZ38Fr+g3nkdep25+oEsmx30IkTYvQVMFZKpK3WWMYUWjWgEzOSvhh+3BOg+3UoxBJSNk";

    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;

    // Declare OpMode members.
    private ElapsedTime runtime = new ElapsedTime();

    // Chassis motors
    private DcMotorEx bob;
    private DcMotorEx dylan;
    private DcMotorEx larry;
    private DcMotorEx jerry;

    // Arm motors/servos
    private DcMotorEx barry;
    private Servo garry;
    private Servo sherry;
    private DcMotorEx sheral;

    private float finalConfidence = 0;
    private float elementPosition;
    ArmPositions dropHeight;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();
        // Hardware mapping
        bob = hardwareMap.get(DcMotorEx.class, "front_left_motor");
        dylan = hardwareMap.get(DcMotorEx.class, "front_right_motor");
        larry = hardwareMap.get(DcMotorEx.class, "back_left_motor");
        jerry = hardwareMap.get(DcMotorEx.class, "back_right_motor");
        barry = hardwareMap.get(DcMotorEx.class, "swing_arm_motor");
        garry = hardwareMap.get(Servo.class, "wrist_joint");
        sherry = hardwareMap.get(Servo.class, "claw_servo");
        sheral = hardwareMap.get(DcMotorEx.class, "spin_motor");




        // Wait for the game to begin
        telemetry.addData(">", "Press Play to start op mode");
        telemetry.update();

        dylan.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        jerry.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        bob.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        larry.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        barry.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        dylan.setDirection((DcMotorSimple.Direction.FORWARD));
        jerry.setDirection(DcMotorSimple.Direction.FORWARD);
        bob.setDirection(DcMotorSimple.Direction.REVERSE);
        larry.setDirection(DcMotorSimple.Direction.REVERSE);

        // The TFObjectDetector uses the camera frames from the VuforiaLocalizer, so we create that
        // first.
        initVuforia();
        initTfod();

        if (tfod != null) {
            tfod.activate();

            // The TensorFlow software will scale the input images from the camera to a lower resolution.
            // This can result in lower detection accuracy at longer distances (> 55cm or 22").
            // If your target is at distance greater than 50 cm (20") you can adjust the magnification value
            // to artificially zoom in to the center of image.  For best results, the "aspectRatio" argument
            // should be set to the value of the images used to create the TensorFlow Object Detection model
            // (typically 16/9).
            tfod.setZoom(1, 16.0 / 9.0);
        }

        while (!isStarted()) {
            if (tfod != null) {
                // getUpdatedRecognitions() will return null if no new information is available since
                // the last time that call was made.
                List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                if (updatedRecognitions != null) {
                    telemetry.addData("# Object Detected", updatedRecognitions.size());
                    // step through the list of recognitions and display boundary info.
                    int i = 0;
                    for (Recognition recognition : updatedRecognitions) {
                        telemetry.addData(String.format("label (%d)", i), recognition.getLabel());
                        telemetry.addData(String.format("  left,top (%d)", i), "%.03f , %.03f",
                                recognition.getLeft(), recognition.getTop());
                        telemetry.addData(String.format("  right,bottom (%d)", i), "%.03f , %.03f",
                                recognition.getRight(), recognition.getBottom());

                        if (finalConfidence < recognition.getConfidence())
                        {
                            finalConfidence = recognition.getConfidence();
                            elementPosition = recognition.getRight();
                        }

                        i++;
                    }
                    telemetry.update();
                }
            }
        }

        tfod.deactivate();
        runtime.reset();

        if (elementPosition > 0 && elementPosition < 275){
            dropHeight = ArmPositions.BOTTOM;
        } else if (elementPosition >= 275 && elementPosition <= 460){
            dropHeight = ArmPositions.MIDDLE;
        } else if (elementPosition > 460 && elementPosition < 645){
            dropHeight = ArmPositions.TOP;
        }

        //code goes here ------------------------ its hardcoded :[
        //set claw and pickup block
        Arm(ArmPositions.PICKUP,.8);
        Claw(1);
        sleep(1000);
        Arm(ArmPositions.BOTTOM,.8);
        //drive to tower
        DrivePlaces("FORWARD", .8, 1200);
        TurnPlacesNew("LEFTFRONT",.8,700);
        //drop block in tower
        Claw(0);
        sleep(2000);
        //turn back towards spin and drive there
        TurnPlacesNew("LEFTBACK",.8,1000);
        DrivePlaces("BACKWARD", .8, 4500);
        //spin duck off
        spin( 1000);
        //




        //code goes here ------------------------




        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            telemetry.addData("encoder value", 4);
            telemetry.update();
        }
    }

    private void DrivePlaces (String direction, double speed, int distance)
    {
        direction = direction.toUpperCase();

        distance = abs(distance);
        switch (direction)
        {
            case "STOP":
                // Stop
                dylan.setPower(0);
                jerry.setPower(0);
                bob.setPower(0);
                larry.setPower(0);
                break;
            case "FORWARD":
                // Drive forward
                dylan.setTargetPosition(distance);
                jerry.setTargetPosition(distance);
                bob.setTargetPosition(distance);
                larry.setTargetPosition(distance);
                break;
            case "FORWARD/RIGHT":
                // Drive forward/right
                dylan.setTargetPosition(distance);
                jerry.setTargetPosition(0);
                bob.setTargetPosition(0);
                larry.setTargetPosition(distance);
                break;
            case "RIGHT":
                // Drive right
                dylan.setTargetPosition(distance);
                jerry.setTargetPosition(-distance);
                bob.setTargetPosition(-distance);
                larry.setTargetPosition(distance);
                break;
            case "BACKWARD/RIGHT":
                // Drive backward/right
                dylan.setTargetPosition(0);
                jerry.setTargetPosition(-distance);
                bob.setTargetPosition(-distance);
                larry.setTargetPosition(0);
                break;
            case "BACKWARD":
                // Drive backward
                dylan.setTargetPosition(-distance);
                jerry.setTargetPosition(-distance);
                bob.setTargetPosition(-distance);
                larry.setTargetPosition(-distance);
                break;
            case "BACKWARD/LEFT":
                // Drive backward/left
                dylan.setTargetPosition(-distance);
                jerry.setTargetPosition(0);
                bob.setTargetPosition(0);
                larry.setTargetPosition(-distance);
                break;
            case "LEFT":
                // Drive left
                dylan.setTargetPosition(-distance);
                jerry.setTargetPosition(distance);
                bob.setTargetPosition(distance);
                larry.setTargetPosition(-distance);
                break;
            case "FORWARD/LEFT":
                // Drive forward/left
                dylan.setTargetPosition(0);
                jerry.setTargetPosition(distance);
                bob.setTargetPosition(distance);
                larry.setTargetPosition(0);
                break;
            default:
                dylan.setTargetPosition(0);
                jerry.setTargetPosition(0);
                bob.setTargetPosition(0);
                larry.setTargetPosition(0);
                break;
        }

        dylan.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        jerry.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        bob.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        larry.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        dylan.setPower(speed);
        jerry.setPower(speed);
        bob.setPower(speed);
        larry.setPower(speed);

        while (opModeIsActive() && (dylan.isBusy()|| jerry.isBusy()|| bob.isBusy()|| larry.isBusy()))
        {
            telemetry.addData("encoder-fwd-left", dylan.getCurrentPosition() + "  busy=" + dylan.isBusy());
            telemetry.addData("encoder-fwd-right", dylan.getCurrentPosition() + "  busy=" + dylan.isBusy());
            telemetry.update();
            idle();
        }
        dylan.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        jerry.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        bob.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        larry.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

    }

    private void TurnPlacesNew (String direction, double speed, int mSecs)
    {

        dylan.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        jerry.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        bob.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        larry.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        direction = direction.toUpperCase();

        if (direction == "RIGHTFRONT")
        {
            dylan.setPower(0);
            jerry.setPower(0);
            bob.setPower(speed);
            larry.setPower(speed);
        }
        else if (direction == "RIGHTBACK")
        {
            dylan.setPower(0);
            jerry.setPower(0);
            bob.setPower(-speed);
            larry.setPower(-speed);
        }
        else if (direction == "LEFTFRONT")
        {
            dylan.setPower(speed);
            jerry.setPower(speed);
            bob.setPower(0);
            larry.setPower(0);
        }
        else if (direction == "LEFTBACK")
        {
            dylan.setPower(-speed);
            jerry.setPower(-speed);
            bob.setPower(0);
            larry.setPower(0);
        }

        sleep(mSecs);

        dylan.setPower(0);
        jerry.setPower(0);
        bob.setPower(0);
        larry.setPower(0);
    }

    public void Arm(ArmPositions position, double power) {
        switch (position) {
            //set arm position and wrist position
            case START:
                barry.setTargetPosition(0);
                garry.setPosition(barry.getTargetPosition() / 1700.0);
                break;
            case PICKUP:
                barry.setTargetPosition(2);
                garry.setPosition(0.3569);
                break;
            case BOTTOM:
                barry.setTargetPosition(300);
                garry.setPosition(barry.getTargetPosition() / 1400.0);
                break;
            case MIDDLE:
                barry.setTargetPosition(600);
                garry.setPosition(barry.getTargetPosition() / 1400.0);
                break;
            case TOP:
                barry.setTargetPosition(1400);
                garry.setPosition(barry.getTargetPosition() / 1700.0);
                break;
            case TOPTOP:
                barry.setTargetPosition(1700);
                garry.setPosition(barry.getTargetPosition() / 1700.0);
                break;
            default:

                break;


        }
        barry.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        barry.setPower(power);
        sleep(2000);
    }

    //0 is ____
    public void Claw(float position) {
        sherry.setPosition(position);
    }

    public void spin(int mSecs){
        sheral.setPower(0);
        sleep(mSecs);
    }

    public void placeFreight(ArmPositions position){
        if(position == ArmPositions.BOTTOM){

        } else if (position == ArmPositions.MIDDLE) {


        } else if (position == ArmPositions.TOP){

        }
    }

    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the TensorFlow Object Detection engine.
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.8f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 320;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
    }
}

