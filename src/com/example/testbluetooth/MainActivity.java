package com.example.testbluetooth;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "BluetoothSimple";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    TextView tvStatus;
    EditText etMessage;
    ListView listDataIncoming;
    Button btnPair, btnData1, btnData2, btnData3, btnData4, btnSend;
    
    ArrayList<String> arr_list;
    String mConnectedDeviceName = null;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothService mChatService = null;
    
    boolean isConnected = false;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "+++ ON CREATE +++");        
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent 
                    = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) 
                setupChat();
        }
    }
    
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
              mChatService.start();
            }    
        }
    }

    public void setupChat() {
        tvStatus = (TextView)findViewById(R.id.tvStatus);
        etMessage = (EditText)findViewById(R.id.etMessage);
        
        arr_list = new ArrayList<String>();
        listDataIncoming = (ListView)findViewById(R.id.listDataIncoming);
        listDataIncoming.setAdapter(new ArrayAdapter(MainActivity.this
                , android.R.layout.simple_list_item_1, arr_list));

        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage(etMessage.getText().toString());
                etMessage.setText("");
            }
        });
        
        btnData1 = (Button)findViewById(R.id.btnData1);
        btnData1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage("Hello");
                Log.e("seng message", "hello");
            }
        });
        
        btnData2 = (Button)findViewById(R.id.btnData2);
        btnData2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage("0123");
                Log.e("seng message", "0123");
            }
        });
        
        btnPair = (Button)findViewById(R.id.btnPair);
        btnPair.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(!isConnected) {
                    Intent serverIntent = new Intent(MainActivity.this
                            , SelectDevice.class);
                    startActivityForResult(serverIntent
                            , REQUEST_CONNECT_DEVICE_SECURE);
                } else {
                    mChatService.stop();
                    mChatService = new BluetoothService(MainActivity.this, mHandler);
                    isConnected = false;
                }
            }
        });
        
        mChatService = new BluetoothService(MainActivity.this, mHandler);
    }

    public void onDestroy() {
        super.onDestroy();
        
        if (mChatService != null) 
            mChatService.stop();
    }

    public void sendMessage(String message) {
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(MainActivity.this, "Device is not connected"
                    , Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    public final void setStatus(String subTitle) {
        tvStatus.setText(subTitle);
    }

    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus("Connected to " + mConnectedDeviceName);
                    isConnected = true;
                    break;
                case BluetoothService.STATE_CONNECTING:
                    setStatus("Connecting...");
                    isConnected = false;
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    setStatus("Not Connected");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                
                Log.e("*****coming message *****", readMessage);
                int[] testarr = bytearray2intarray(readBuf);
                for(int i=0; i<testarr.length;i++){
                	Log.e("*****coming message2 *****", Integer.toString(testarr[i]));
                }
                arr_list.add(readMessage);
            	 //txtShow = (TextView) findViewById(R.id.txtShow);
            	 /*StringBuilder sb = new StringBuilder();
            	 byte[] readBuf = (byte[]) msg.obj;
            	 String readData = new String(readBuf, 0, msg.arg1);
            	 sb.append(readData);
            	 int endOfLineIndex = sb.indexOf("\n");
            	 
            	 if(endOfLineIndex > 0){
            		 String sbprint = sb.substring(0, endOfLineIndex);
            		 Log.e("***** test comming message *****", sbprint);
            		 sb.delete(0, sb.length());
            		 //txtShow.setText(sbprint);
            		 Log.d(TAG, "Start BT Send");
            		 //updateAuthen(sbprint);
            		 Log.d(TAG, "End BT Send");
            	 }*/

                break;
            case MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext()
                               , "Connected to " + mConnectedDeviceName
                               , Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext()
                               , msg.getData().getString(TOAST)
                               , Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            if (resultCode == Activity.RESULT_OK)
                connectDevice(data, true);
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            if (resultCode == Activity.RESULT_OK)
                connectDevice(data, false);
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupChat();
            } else {
                Toast.makeText(MainActivity.this
                        , "Bluetooth was not enabled. Leaving Bluetooth Chat"
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras()
            .getString(SelectDevice.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }
    
    public int[] bytearray2intarray(byte[] barray)
    {
      int[] iarray = new int[barray.length];
      int i = 0;
      for (byte b : barray)
          iarray[i++] = b & 0xff;
      // "and" with 0xff since bytes are signed in java
      return iarray;
    }
}