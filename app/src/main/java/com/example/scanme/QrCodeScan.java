package com.example.scanme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;

public class QrCodeScan extends Fragment implements ZXingScannerView.ResultHandler {
    private static final String TAG = "HI";
    private ZXingScannerView mScannerView;
    private static final int REQUEST_CAMERA =1;

    List<MainData> dataList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    RoomDB database;
    public QrCodeScan() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScannerView = new ZXingScannerView(getContext());
        getActivity().setContentView(mScannerView);

        // Initialize database
        database = RoomDB.getInstance(getActivity());

        //Store database value in data list
        dataList = database.mainDao().getAll();

        // Initialize linear layout manager
        linearLayoutManager = new LinearLayoutManager(getActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                Toast.makeText(getContext(), "Permission is granted", Toast.LENGTH_SHORT).show();
            }
            else {
                requestPermission();
            }
        }
    }

    private void mScannerView(FragmentActivity activity) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.qr_code_scan, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(getActivity(), CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA}, REQUEST_CAMERA);
    }

    public void onRequestPermissionsResult(int requestCode, String permission[], int grantResults[]){
        switch (requestCode){
            case REQUEST_CAMERA:
                if(grantResults.length>0){
                    boolean cameraAccepted =grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted){
                        Toast.makeText(getActivity(), "Permission Granted ", Toast.LENGTH_LONG).show();

                    }
                    else{
                        Toast.makeText(getActivity(), "Permission Denied ", Toast.LENGTH_LONG).show();
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                            if(shouldShowRequestPermissionRationale(CAMERA)){
                                displayAlertMessage("You need to allow access for both permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA);
                                            }
                                        });
                                return;

                            }
                        }
                    }
                }
                break;
        }

    }

    public void displayAlertMessage(String message, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void handleResult(Result result) {
        final String scanResult = result.getText().trim();
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        builder.setTitle("Scan Result is in the following");
        builder.setNegativeButton("DISCOVER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                database.mainDao().reset(dataList);

                //initialize main data
                MainData data = new MainData();

                //Get current date and time;
                long time =  System.currentTimeMillis();
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                String dateAndTime = formatter.format(new Date(Long.parseLong(String.valueOf(time))));

                //Set text on main data
                data.setText(scanResult);

                //Insert text in database
                database.mainDao().insert(data);

                dataList.addAll(database.mainDao().getAll());
                mScannerView.resumeCameraPreview(QrCodeScan.this::handleResult);

            }
        });

        builder.setMessage(scanResult);
        AlertDialog alert = builder.create();
        alert.show();

    }}