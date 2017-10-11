/*
 * Copyright (C) 2016 SecuGen Corporation
 *
 */

package com.secugen.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import SecuGen.Driver.Constant;
import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGANSITemplateInfo;
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier;
import SecuGen.FDxSDKPro.SGDeviceInfoParam;
import SecuGen.FDxSDKPro.SGFDxConstant;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPresentEvent;
import SecuGen.FDxSDKPro.SGISOTemplateInfo;
import SecuGen.FDxSDKPro.SGImpressionType;
import SecuGen.FDxSDKPro.SGWSQLib;
import com.secugen.demo.R;

public class JSGDActivity extends FragmentActivity
        implements View.OnClickListener, SGFingerPresentEvent,UserInfoFragment.EnrollUserDialogListener {

    private static final String TAG = "SecuGen USB";
    private int count =0;
	//Capture button
    private Button mButtonCapture;
	//Register button
    private Button mButtonRegister;
	//match button
    private Button mButtonMatch;
	//Enable led in fingerprint device
    private Button mButtonLed;
    private Button mSDKTest;

    private EditText mEditLog;
	SharedPreferences sharedPreferences;

    private android.widget.TextView mTextViewResult;
  //  private android.widget.CheckBox mCheckBoxMatched;
    private android.widget.ToggleButton mToggleButtonSmartCapture;
    private android.widget.ToggleButton mToggleButtonCaptureModeN;
    private android.widget.ToggleButton mToggleButtonAutoOn;
    private android.widget.ToggleButton mToggleButtonNFIQ;
    private android.widget.ToggleButton mToggleButtonUSBBulkMode64;
    private PendingIntent mPermissionIntent;
    private ImageView mImageViewFingerprint;
    private ImageView mImageViewRegister;
    private ImageView mImageViewVerify;
    private byte[] mRegisterImage;
    private byte[] mVerifyImage;
    private byte[] mRegisterTemplate;
    private byte[] mVerifyTemplate;
	private int[] mMaxTemplateSize;
	private int mImageWidth;
	private int mImageHeight;
	private int mImageDPI;
	private int[] grayBuffer;
    private Bitmap grayBitmap;
    private IntentFilter filter; //2014-04-11
    private SGAutoOnEventNotifier autoOn;
  //  private boolean mLed;
    private boolean mAutoOnEnabled;
   // private int nCaptureModeN;
    private Button mButtonSetBrightness0;
    private Button mButtonSetBrightness100;
    private Button mButtonReadSN;
    private boolean bSecuGenDeviceOpened;
    private JSGFPLib sgfplib;
    private boolean usbPermissionRequested;

    private void debugMessage(String message) {
        this.mEditLog.append(message);
        this.mEditLog.invalidate(); //TODO trying to get Edit log to update after each line written
    }

    //This broadcast receiver is necessary to get user permissions to access the attached USB device
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();

    		if (ACTION_USB_PERMISSION.equals(action)) {
    			synchronized (this) {
    				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
    					if(device != null){

    						debugMessage("USB BroadcastReceiver VID : " + device.getVendorId() + "\n");
    						debugMessage("USB BroadcastReceiver PID: " + device.getProductId() + "\n");
    					}
    					else
        					Log.e(TAG, "mUsbReceiver.onReceive() Device is null");
    				}
    				else
    					Log.e(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
    			}
    		}
    	}
    };

    //This message handler is used to access local resources not
    //accessible by SGFingerPresentCallback() because it is called by
    //a separate thread.
    public Handler fingerDetectedHandler = new Handler(){

		// @Override
	    public void handleMessage(Message msg) {
	       CaptureFingerPrint();
	    	if (mAutoOnEnabled) {
				mToggleButtonAutoOn.toggle();
		    	EnableControls();
	    	}
	    }
    };

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
	public void EnableControls(){
		//Toast.makeText(JSGDActivity.this,"Enable controls is called",Toast.LENGTH_LONG).show();
		//this.mButtonCapture.setClickable(true);
		//this.mButtonCapture.setTextColor(getResources().getColor(android.R.color.white));
		this.mButtonRegister.setClickable(true);
		this.mButtonRegister.setTextColor(getResources().getColor(android.R.color.white));
		this.mButtonMatch.setClickable(true);
		this.mButtonMatch.setTextColor(getResources().getColor(android.R.color.white));
	    mButtonSetBrightness0.setClickable(true);
	    mButtonSetBrightness100.setClickable(true);
	   // mButtonReadSN.setClickable(true);
		///this.mButtonLed.setClickable(true);
		//this.mButtonLed.setTextColor(getResources().getColor(android.R.color.white));
	}

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
	public void DisableControls(){
		//Toast.makeText(JSGDActivity.this,"Disable controls is called",Toast.LENGTH_LONG).show();
		//this.mButtonCapture.setClickable(false);
	//	this.mButtonCapture.setTextColor(getResources().getColor(android.R.color.black));
		this.mButtonRegister.setClickable(false);
		this.mButtonRegister.setTextColor(getResources().getColor(android.R.color.black));
		this.mButtonMatch.setClickable(false);
		this.mButtonMatch.setTextColor(getResources().getColor(android.R.color.black));
	    mButtonSetBrightness0.setClickable(false);;
	    mButtonSetBrightness100.setClickable(false);;
	  //  mButtonReadSN.setClickable(false);
		//this.mButtonLed.setClickable(false);
		//this.mButtonLed.setTextColor(getResources().getColor(android.R.color.black));
	}


    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//Log.d(TAG, "onCreate()");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);
		//Toast.makeText(JSGDActivity.this,"on create is called",Toast.LENGTH_LONG).show();
		sharedPreferences = getSharedPreferences("fingerprint_data", MODE_PRIVATE);
        mButtonCapture = (Button)findViewById(R.id.buttonCapture);
        mButtonCapture.setOnClickListener(this);
        mButtonRegister = (Button)findViewById(R.id.buttonRegister);
        mButtonRegister.setOnClickListener(this);
        mButtonMatch = (Button)findViewById(R.id.buttonMatch);
        mButtonMatch.setOnClickListener(this);
      //  mButtonLed = (Button)findViewById(R.id.buttonLedOn);
        //mButtonLed.setOnClickListener(this);
        //mSDKTest = (Button)findViewById(R.id.buttonSDKTest);
        //mSDKTest.setOnClickListener(this);
        mEditLog = (EditText)findViewById(R.id.editLog);
        mTextViewResult = (android.widget.TextView)findViewById(R.id.textViewResult);
        //mCheckBoxMatched = (android.widget.CheckBox) findViewById(R.id.checkBoxMatched);
       // mToggleButtonSmartCapture = (android.widget.ToggleButton) findViewById(R.id.toggleButtonSmartCapture);
       // mToggleButtonSmartCapture.setOnClickListener(this);
       // mToggleButtonCaptureModeN = (android.widget.ToggleButton) findViewById(R.id.toggleButtonCaptureModeN);
       // mToggleButtonCaptureModeN.setOnClickListener(this);
        mToggleButtonAutoOn = (android.widget.ToggleButton) findViewById(R.id.toggleButtonAutoOn);
        mToggleButtonAutoOn.setOnClickListener(this);
        //mToggleButtonNFIQ = (android.widget.ToggleButton) findViewById(R.id.toggleButtonNFIQ);
        //mToggleButtonNFIQ.setOnClickListener(this);
        //mToggleButtonUSBBulkMode64 = (android.widget.ToggleButton) findViewById(R.id.ToggleButtonUSBBulkMode64);
        //mToggleButtonUSBBulkMode64.setOnClickListener(this);
        mImageViewFingerprint = (ImageView)findViewById(R.id.imageViewFingerprint);
        mImageViewRegister = (ImageView)findViewById(R.id.imageViewRegister);
        mImageViewVerify = (ImageView)findViewById(R.id.imageViewVerify);
	    mButtonSetBrightness0 = (Button)findViewById(R.id.buttonSetBrightness0);
	    mButtonSetBrightness0.setOnClickListener(this);
	    mButtonSetBrightness100 = (Button)findViewById(R.id.buttonSetBrightness100);
	    mButtonSetBrightness100.setOnClickListener(this);
		mButtonSetBrightness0.setClickable(false);
		mButtonSetBrightness100.setClickable(false);
		mButtonSetBrightness0.setTextColor(getResources().getColor(android.R.color.black));
		mButtonSetBrightness100.setTextColor(getResources().getColor(android.R.color.black));
	//	mButtonReadSN = (Button)findViewById(R.id.buttonReadSN);
	//	mButtonReadSN.setOnClickListener(this);

		//Setting gray colored bitmap in fingerprint image view at initial phase
        grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES*JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
        for (int i=0; i<grayBuffer.length; ++i)
        	grayBuffer[i] = Color.GRAY;
		//Return mutable bitmap with mentioned height and width
        grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
        grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);

        mImageViewFingerprint.setImageBitmap(grayBitmap);

		//Setting gray color image in register and verify image view which is half of the actual image view for fingerprint
        int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2)*(JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2)];
        for (int i=0; i<sintbuffer.length; ++i)
        	sintbuffer[i] = Color.GRAY;

        Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2, Bitmap.Config.ARGB_8888);
        sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2);
        mImageViewRegister.setImageBitmap(grayBitmap);
        mImageViewVerify.setImageBitmap(grayBitmap);

        mMaxTemplateSize = new int[1];

        //USB Permissions
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
       	filter = new IntentFilter(ACTION_USB_PERMISSION);
		//Uncomm
		registerReceiver(mUsbReceiver, filter);
        sgfplib = new JSGFPLib((UsbManager)getSystemService(Context.USB_SERVICE));

        //For testing purpose
//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//        while(deviceIterator.hasNext()){
//            UsbDevice device = deviceIterator.next();
//            //Saving data to shared preferance for later verification
//
//            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
//            dlgAlert.setMessage("Your usb device is "+device.toString());
//            dlgAlert.setTitle("USB Device info");
//            dlgAlert.setPositiveButton("OK",
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog,int whichButton){
//                            //	finish();
//                            return;
//                        }
//                    }
//            );
//            dlgAlert.setCancelable(false);
//            dlgAlert.create().show();
//        }

       // this.mToggleButtonSmartCapture.toggle();
        bSecuGenDeviceOpened = false;
        usbPermissionRequested = false;

		debugMessage("Starting Activity\n");
		debugMessage("jnisgfplib version: " + Integer.toHexString((int)sgfplib.Version()) + "\n");
		//mLed = false;
		mAutoOnEnabled = false;
		autoOn = new SGAutoOnEventNotifier (sgfplib, this);
		//nCaptureModeN = 0;
		//Toast.makeText(JSGDActivity.this,"on create end  is called",Toast.LENGTH_LONG).show();
    }
    public void onClick(View v) {
        try {
            long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;

            if (v == mToggleButtonAutoOn) {
                if (mToggleButtonAutoOn.isChecked()) {
                    mAutoOnEnabled = true;
                    autoOn.start(); //Enable Auto On
                    DisableControls();
                } else {
                    mAutoOnEnabled = false;
                    autoOn.stop(); //Disable Auto On
                    EnableControls();
                }

            }
            if (v == this.mButtonRegister) {
                //DEBUG Log.d(TAG, "Clicked REGISTER");
                //Toast.makeText(JSGDActivity.this,"registe button click start is called",Toast.LENGTH_LONG).show();
                debugMessage("Clicked REGISTER\n");
                if (mRegisterImage != null)
                    mRegisterImage = null;
                mRegisterImage = new byte[mImageWidth * mImageHeight];

                //	this.mCheckBoxMatched.setChecked(false);
                dwTimeStart = System.currentTimeMillis();
                long result = sgfplib.GetImage(mRegisterImage);

//                FileOutputStream outStream = null;
//
//                // Write to SD Card
//                try {
//                    File sdCard = Environment.getExternalStorageDirectory();
//                    File dir = new File(sdCard.getAbsolutePath() + "/camtest");
//                    dir.mkdirs();
//
//                    String fileName = String.format("d"+count+".jpg", System.currentTimeMillis());
//                    File outFile = new File(dir, fileName);
//
//                    outStream = new FileOutputStream(outFile);
//                    this.toGrayscale(mRegisterImage).compress(Bitmap.CompressFormat.JPEG, 100, outStream);
//                    outStream.flush();
//                    outStream.close();
//
//                    Log.d(TAG, "onPictureTaken - wrote to " + outFile.getAbsolutePath());
//
//                    //refreshGallery(outFile);
//                } catch (FileNotFoundException e) {
//                    //print("FNF");
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                }
                //saveToInternalStorage(this.toGrayscale(mRegisterImage),"test"+count+".jpg");

//                new ImageSaver(this).
//                        setFileName("test"+count+".jpg").
//                        setDirectoryName("secugenimages").
//                        save(this.toGrayscale(mRegisterImage));
                count++;
                //String resultStr=""+result;
                //Toast.makeText(JSGDActivity.this,resultStr,Toast.LENGTH_LONG).show();

                //DumpFile("register.raw", mRegisterImage);

                mImageViewFingerprint.setImageBitmap(this.toGrayscale(mRegisterImage));
                dwTimeStart = System.currentTimeMillis();
                result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);

                SGFingerInfo fpInfo = new SGFingerInfo();
                for (int i = 0; i < mRegisterTemplate.length; ++i)
                    mRegisterTemplate[i] = 0;
                dwTimeStart = System.currentTimeMillis();
                result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);

                //Showing alert dialog for displaying register template


                //Saving data to shared preferance for later verification
                String tempUsername= "user"+ Math.random();
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("Your random user name is "+tempUsername);
                dlgAlert.setTitle("Enrollment info");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int whichButton){
                                //	finish();
                                return;
                            }
                        }
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
                DumpFile(tempUsername,mRegisterTemplate);
//				SharedPreferences.Editor editor = sharedPreferences.edit();
//
//				String templateStr = new String(mRegisterTemplate);
//
//				Toast.makeText(JSGDActivity.this,"Saving Username is ="+tempUsername,Toast.LENGTH_LONG).show();
//				editor.putString(templateStr,tempUsername);
//				editor.commit();

                //DumpFile("register.min", mRegisterTemplate);
                dwTimeEnd = System.currentTimeMillis();
                dwTimeElapsed = dwTimeEnd - dwTimeStart;
                //debugMessage("CreateTemplate() ret:" + result + " [" + dwTimeElapsed + "ms]\n");
                mImageViewRegister.setImageBitmap(this.toGrayscale(mRegisterImage));

                mTextViewResult.setText("Click Verify");
                mRegisterImage = null;
                //	fpInfo = null;
                //Toast.makeText(JSGDActivity.this, "register button click end is called", Toast.LENGTH_LONG).show();
            }
            if (v == this.mButtonMatch) {
                //DEBUG Log.d(TAG, "Clicked MATCH");
                debugMessage("Clicked MATCH\n");
                if (mVerifyImage != null)
                    mVerifyImage = null;
                mVerifyImage = new byte[mImageWidth * mImageHeight];
                dwTimeStart = System.currentTimeMillis();
                long result = sgfplib.GetImage(mVerifyImage);
                //	DumpFile("verify.raw", mVerifyImage);
                dwTimeEnd = System.currentTimeMillis();
                dwTimeElapsed = dwTimeEnd - dwTimeStart;
                debugMessage("GetImage() ret:" + result + " [" + dwTimeElapsed + "ms]\n");
                mImageViewFingerprint.setImageBitmap(this.toGrayscale(mVerifyImage));
                mImageViewVerify.setImageBitmap(this.toGrayscale(mVerifyImage));

                dwTimeStart = System.currentTimeMillis();
                result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
                dwTimeEnd = System.currentTimeMillis();
                dwTimeElapsed = dwTimeEnd - dwTimeStart;
                debugMessage("SetTemplateFormat(SG400) ret:" + result + " [" + dwTimeElapsed + "ms]\n");

                SGFingerInfo fpInfo = new SGFingerInfo();
                for (int i = 0; i < mVerifyTemplate.length; ++i)
                    mVerifyTemplate[i] = 0;
                dwTimeStart = System.currentTimeMillis();


                result = sgfplib.CreateTemplate(fpInfo, mVerifyImage, mVerifyTemplate);
                //DumpFile("verify.min", mVerifyTemplate);
                dwTimeEnd = System.currentTimeMillis();
                dwTimeElapsed = dwTimeEnd - dwTimeStart;
                debugMessage("CreateTemplate() ret:" + result + " [" + dwTimeElapsed + "ms]\n");

                dwTimeStart = System.currentTimeMillis();


                //Iterating our saved data
                //Map<String,?> keys = sharedPreferences.getAll();
                String username=findMatchedUserdetail(mVerifyTemplate);
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("your username is "+username);
                dlgAlert.setTitle("user found");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int whichButton){
                                //	finish();
                                return;
                            }
                        }
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();

                mVerifyImage = null;
                fpInfo = null;
            }
            if (v == this.mButtonSetBrightness0) {
                this.sgfplib.SetBrightness(0);
                debugMessage("SetBrightness(0)\n");
            }
            //Show dialog fragment
            if (v == this.mButtonCapture) {
                showEnrollDialog();
            }
            if (v == this.mButtonSetBrightness100) {
                this.sgfplib.SetBrightness(100);
                debugMessage("SetBrightness(100)\n");
            }
        }catch(Exception e){
            Toast.makeText(JSGDActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
        }
    }
    private String saveToInternalStorage(Bitmap bitmapImage,String fileName){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onPause() {
    	//Log.d(TAG, "onPause()");
		//Toast.makeText(JSGDActivity.this,"On pause start is called",Toast.LENGTH_LONG).show();
    	if (bSecuGenDeviceOpened)
    	{
    		autoOn.stop();
    		EnableControls();
    		sgfplib.CloseDevice();
            bSecuGenDeviceOpened = false;
    	}
    	unregisterReceiver(mUsbReceiver);
    	mRegisterImage = null;
    	mVerifyImage = null;
    	mRegisterTemplate = null;
    	mVerifyTemplate = null;
        mImageViewFingerprint.setImageBitmap(grayBitmap);
        mImageViewRegister.setImageBitmap(grayBitmap);
        mImageViewVerify.setImageBitmap(grayBitmap);
	//	Toast.makeText(JSGDActivity.this,"on pause end is called",Toast.LENGTH_LONG).show();
        super.onPause();

    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onResume(){
    	//Log.d(TAG, "onResume()");

        super.onResume();
		//Toast.makeText(JSGDActivity.this,"on resume start is called",Toast.LENGTH_LONG).show();
        DisableControls();
       	registerReceiver(mUsbReceiver, filter);
        long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE){
        	AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        	if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
        		dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
        	else
        		dlgAlert.setMessage("Fingerprint device initialization failed!");
        	dlgAlert.setTitle("SecuGen Fingerprint SDK");
        	dlgAlert.setPositiveButton("OK",
        			new DialogInterface.OnClickListener() {
        		      public void onClick(DialogInterface dialog,int whichButton){
        		        	//finish();
        		        	return;
        		      }
        			}
        	);
        	dlgAlert.setCancelable(false);
        	dlgAlert.create().show();
        }
        else {
	        UsbDevice usbDevice = sgfplib.GetUsbDevice();
	        if (usbDevice == null){
	        	AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
	        	dlgAlert.setMessage("SecuGen fingerprint sensor not found!");
	        	dlgAlert.setTitle("SecuGen Fingerprint SDK");
	        	dlgAlert.setPositiveButton("OK",
	        			new DialogInterface.OnClickListener() {
	        		      public void onClick(DialogInterface dialog,int whichButton){
	        		        //	finish();
	        		        	return;
	        		      }
	        			}
	        	);
	        	dlgAlert.setCancelable(false);
	        	dlgAlert.create().show();
	        }
	        else {
	        	boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
		        if (!hasPermission) {
					Toast.makeText(JSGDActivity.this,"has permisson not start is called",Toast.LENGTH_LONG).show();
			        if (!usbPermissionRequested)
			        {
			    		debugMessage("Requesting USB Permission\n");
			        	//Log.d(TAG, "Call GetUsbManager().requestPermission()");
			        	usbPermissionRequested = true;
			        	sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
			        }
			        else
			        {
			        	//wait up to 20 seconds for the system to grant USB permission
			        	hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
			    		debugMessage("Waiting for USB Permission\n");
			        	int i=0;
				        while ((hasPermission == false) && (i <= 40))
				        {
				        	++i;
				            hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
				        	try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
				        	//Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
				        }
			        }
					//Toast.makeText(JSGDActivity.this,"has permisson not start end is called",Toast.LENGTH_LONG).show();
		        }
		        if (hasPermission) {
					//Toast.makeText(JSGDActivity.this,"has permisson start is called",Toast.LENGTH_LONG).show();
		    		debugMessage("Opening SecuGen Device\n");
			        error = sgfplib.OpenDevice(0);
					debugMessage("OpenDevice() ret: " + error + "\n");
					if (error == SGFDxErrorCode.SGFDX_ERROR_NONE)
					{
				        bSecuGenDeviceOpened = true;
						SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
				        error = sgfplib.GetDeviceInfo(deviceInfo);
						debugMessage("GetDeviceInfo() ret: " + error + "\n");
				    	mImageWidth = deviceInfo.imageWidth;
				    	mImageHeight= deviceInfo.imageHeight;
				    	mImageDPI = deviceInfo.imageDPI;
				        sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
						sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
						debugMessage("TEMPLATE_FORMAT_SG400 SIZE: " + mMaxTemplateSize[0] + "\n");
				        mRegisterTemplate = new byte[mMaxTemplateSize[0]];
				        mVerifyTemplate = new byte[mMaxTemplateSize[0]];
				        EnableControls();
				      //  boolean smartCaptureEnabled = this.mToggleButtonSmartCapture.isChecked();
//				        if (smartCaptureEnabled)
//				        	sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte)1);
//				        else
				        	sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte)0);
				        if (mAutoOnEnabled){
				        	autoOn.start();
				        	DisableControls();
				        }
			        }
			        else
			        {
						debugMessage("Waiting for USB Permission\n");
			        }
				//	Toast.makeText(JSGDActivity.this,"haspermisson end is called",Toast.LENGTH_LONG).show();
		        }
		        //Thread thread = new Thread(this);
		        //thread.
				// start();
	        }

        }
		//Toast.makeText(JSGDActivity.this,"on resume end is called",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
		sgfplib.CloseDevice();
    	mRegisterImage = null;
    	mVerifyImage = null;
    	mRegisterTemplate = null;
    	mVerifyTemplate = null;
    	sgfplib.Close();
       	unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer, int width, int height)
    {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
                        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                        Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer) {

        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
                        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                        Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y=0; y< height; ++y) {
            for (int x=0; x< width; ++x){
            	int color = bmpOriginal.getPixel(x, y);
            	int r = (color >> 16) & 0xFF;
            	int g = (color >> 8) & 0xFF;
            	int b = color & 0xFF;
            	int gray = (r+g+b)/3;
            	color = Color.rgb(gray, gray, gray);
            	//color = Color.rgb(r/3, g/3, b/3);
            	bmpGrayscale.setPixel(x, y, color);
            }
        }
        return bmpGrayscale;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //Converts image to binary (OLD)
    public Bitmap toBinary(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }



    public void DumpFile(String fileName, byte[] buffer)
    {
    	//Uncomment section below to dump images and templates to SD card
        try {

			ContextWrapper cw = new ContextWrapper(getApplicationContext());
			// path to /data/data/yourapp/app_data/imageDir
			File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
			//Toast.makeText(JSGDActivity.this,directory.toString()+"  dumpfile directory",Toast.LENGTH_LONG);
			// Create imageDir
			File myFile=new File(directory,fileName);
           // File myFile = new File("/Download/" + fileName);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            fOut.write(buffer,0,buffer.length);
            fOut.close();
        } catch (Exception e) {
            Toast.makeText(JSGDActivity.this,e.toString()+" "+fileName,Toast.LENGTH_LONG).show();
        }
    }

	private String findMatchedUserdetail(byte[] verifyTemplate){
		String username = "";
		try {

			ContextWrapper context = new ContextWrapper(getApplicationContext());
			File mydir = context.getDir("imageDir", Context.MODE_PRIVATE);
			File lister = mydir.getAbsoluteFile();
			//Toast.makeText(JSGDActivity.this, mydir.toString(), Toast.LENGTH_SHORT).show();
			for (String list : lister.list()) {
				File file = new File(mydir,list);
				FileInputStream fin = null;
				//Toast.makeText(JSGDActivity.this,list,Toast.LENGTH_LONG).show();
				try {
					// create FileInputStream object
					fin = new FileInputStream(file);

					byte fileContent[] = new byte[(int) file.length()];
					// Reads up to certain bytes of data from this input stream into an array of bytes.
					fin.read(fileContent);
					boolean[] matched = new boolean[1];
					AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
					dlgAlert.setMessage("verify template= "+mVerifyTemplate+" \n file content="+fileContent);
					dlgAlert.setTitle("Template detail");
					dlgAlert.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int whichButton){
									//	finish();
									return;
								}
							}
					);
					dlgAlert.setCancelable(false);
				//	dlgAlert.create().show();
					long result = sgfplib.MatchTemplate(fileContent, mVerifyTemplate, SGFDxSecurityLevel.SL_NORMAL, matched);

					if (matched[0]) {
						mTextViewResult.setText("MATCHED!!\n");

						//Toast.makeText(JSGDActivity.this, list, Toast.LENGTH_LONG).show();
						//this.mCheckBoxMatched.setChecked(true);
						debugMessage("MATCHED!!\n");
						return list;
					} else {
						mTextViewResult.setText("NOT MATCHED!!");
						//this.mCheckBoxMatched.setChecked(false);
						debugMessage("NOT MATCHED!!\n");

					}
				}catch (Exception e){
					Toast.makeText(JSGDActivity.this,e.toString()+" find match eduserdetail 1",Toast.LENGTH_LONG).show();
				}
			}
		}catch(Exception e){
			Toast.makeText(JSGDActivity.this,e.toString()+" find match eduserdetail",Toast.LENGTH_LONG).show();
		}
		return username;
	}
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    public void SGFingerPresentCallback (){
		//Toast.makeText(JSGDActivity.this,"finger present callback is called",Toast.LENGTH_LONG).show();
		autoOn.stop();
		fingerDetectedHandler.sendMessage(new Message());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
	public void CaptureFingerPrint(){
		//Toast.makeText(JSGDActivity.this,"capture finger print start is called",Toast.LENGTH_LONG).show();
		long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
		//this.mCheckBoxMatched.setChecked(false);
	    byte[] buffer = new byte[mImageWidth*mImageHeight];
	    dwTimeStart = System.currentTimeMillis();
	    //long result = sgfplib.GetImage(buffer);
	    long result = sgfplib.GetImageEx(buffer, 10000,50);
	    String NFIQString;
	    	NFIQString = "";
	    	//DumpFile("capture2016.raw", buffer);
	    	dwTimeEnd = System.currentTimeMillis();
	    	dwTimeElapsed = dwTimeEnd-dwTimeStart;
	    	debugMessage("getImageEx(10000,50) ret:" + result + " [" + dwTimeElapsed + "ms]" + NFIQString +"\n");
			mTextViewResult.setText("getImageEx(10000,50) ret: " + result + " [" + dwTimeElapsed + "ms] " + NFIQString +"\n");
	    	mImageViewFingerprint.setImageBitmap(this.toGrayscale(buffer));
			buffer = null;
		//Toast.makeText(JSGDActivity.this,"capture finger print end is called",Toast.LENGTH_LONG).show();
	}



	public void showEnrollDialog() {
		// Create an instance of the dialog fragment and show it
		DialogFragment dialog = new UserInfoFragment();
		dialog.show(getSupportFragmentManager(), "UserInfoFragment");
	}

	// The dialog fragment receives a reference to this Activity through the
	// Fragment.onAttach() callback, which it uses to call the following methods
	// defined by the NoticeDialogFragment.NoticeDialogListener interface
	@Override
	public void onDialogPositiveClick(DialogFragment dialogFragment,DialogInterface dialog) {
		// User touched the dialog's positive button
	 System.out.println("Dialog ok button clicked  information is="+dialog.toString());
        Dialog currentDialog=(Dialog)dialog;

        EditText usernameText=(EditText) currentDialog.findViewById(R.id.username);
        System.out.println("I am inside the enroll block and username is="+usernameText.getText().toString());

	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// User touched the dialog's negative button
		System.out.println("Dialog cancel button is information is="+dialog.toString());
        dialog.getDialog().cancel();
	}

}