package com.example.rssitester;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class MainActivity extends Activity implements LeScanCallback {
    private static final String TAG = "BluetoothGattActivity";

	private BluetoothAdapter mBluetoothAdapter;
	private SparseArray<BluetoothDevice> mDevices;
	private BluetoothGatt mConnectedGatt;
	private TextView tv;
	private EditText m;
	private String current;
	private AlertDialog ad;
	UPDwork udP = new UPDwork();
	int count = 0;
	int[] record_rss = new int [7000]; //max 1000
	int record_times = 1;

	Socket socket;
	PrintWriter os;

	public void initsocket(){

		try {
			socket=new Socket("127.0.0.1",6000);
			os=new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendMsg(String msg){
		os.println(msg);
		os.flush();
	}


	public void closeSocket(){
		os.close(); //¹Ø±ÕSocketÊä³öÁ÷
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv = (TextView) findViewById(R.id.bName);
		//m = (EditText) findViewById(R.id.meters);
		//udP.setHost("172.22.136.197");
		//udP.send("1");
		
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();
        ad = new AlertDialog.Builder(this).create();
        ad.setMessage("Scan Complete");
        ad.setButton(DialogInterface.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		dialog.dismiss();
        	}
        });

		initsocket();
	}


	@Override
	protected void onResume(){
		super.onResume();
		if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
			finish();
			return;
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		//udP.send("stop");
		//stopScan();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
            	//stopScan();
            	startScan();
                return true;
            case R.id.action_save:
            	stopScan();
            	Intent sendIntent = new Intent();
            	sendIntent.setAction(Intent.ACTION_SEND);
            	sendIntent.putExtra(Intent.EXTRA_TEXT, current);
            	sendIntent.setType("text/plain");
            	startActivity(sendIntent);
            	return true;
            case R.id.action_clear:
            	tv.setText("");
            	current = "";
            	stopScan();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
            //ad.show();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

	private boolean flag = true;
	private boolean switer = true;
	private  Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(flag){
				mBluetoothAdapter.startLeScan(MainActivity.this);
				Log.e("startLeScan", "startLeScan");
				flag= !flag;

				if (switer){
					handler.sendEmptyMessageDelayed(0,1000);
				}
			} else {
				mBluetoothAdapter.stopLeScan(MainActivity.this);
				Log.e("stopLeScan", "stopLeScan");
				flag= !flag;

				if (switer){
					handler.sendEmptyMessageDelayed(0,100);
				}
			}




		}
	};
	private void startScan(){

		switer=true;
		handler.sendEmptyMessage(0);

		//mBluetoothAdapter.startLeScan(this);
        //Message msg = new Message();
        //String textTochange = " New Scan ";
        //msg.obj = textTochange;
        //mHandler.sendMessage(msg);
        //mHandler.postDelayed(mStopRunnable, 60000);
	}
	
	private void stopScan()  {
		switer=false;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mBluetoothAdapter.stopLeScan(MainActivity.this);

	}
	
	public void dialog() {
		stopScan(); // stop scaning first
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage("Continue?");
		builder.setTitle("Instruction");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
				startScan();
				count = 0;
				record_times++;
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
				//udP.send("stop");
				try {
					writeSDcardFile();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				sendEmail();
			}
		});
		
		builder.create().show();
	}
	
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        /*
         * We are looking for SensorTag devices only, so validate the name
         * that each device reports before adding it to our collection
         */
    	//if(device.getAddress().equals("00:1A:C0:8F:61:04")){
			//int major = (scanRecord[4+20] & 0xff) * 0x100 + (scanRecord[4+21] & 0xff);
			/*for(int i = 0; i < scanRecord.length;i++){
				System.out.println(scanRecord[i]);
			}*/

			String str = device.getAddress();
			String substring = str.substring(Math.max(str.length() - 5, 0));
			Log.e("rssi",""+rssi +" "+substring);
            //Log.i(TAG, "New LE Device: " + device.getAddress() + " @ " + rssi);
			EditText mac = (EditText)findViewById(R.id.MAC);

			if (substring.equals(mac.getText().toString())) { // iBeacon address last 4
				Message msg = new Message();
	            String textTochange = String.valueOf(rssi);//scanRecord[25]+","+scanRecord[28]+","+substring+","+String.valueOf(rssi);//String.valueOf(rssi)
	            msg.obj = textTochange;

	            //stopScan();
				int setCount =150;
	            if (count < setCount) {
	            	//udP.send((String)msg.obj);
	            	//startScan();
	            	if (record_times<14) {
	            		record_rss[(record_times-1)*setCount+count] = rssi;
					}
	            	count++;
				}else {
					dialog();
				}
	            mHandler.sendMessage(msg);
			}
        //}
		
	}
	
	private void writeSDcardFile() throws IOException {
		// TODO Auto-generated method stub
		String dataString;

        //FileOutputStream fou = openFileOutput(Environment.getExternalStorageDirectory().toString()+"/"+"accData.txt", MODE_APPEND+MODE_WORLD_WRITEABLE+MODE_WORLD_READABLE);  
        //OutputStreamWriter out = new OutputStreamWriter(fou);  
        
        OutputStream out = null;
        out=new FileOutputStream(Environment.getExternalStorageDirectory().toString()+"/"+"iBeacon"+".txt");
		
		for (int i = 0; i < record_rss.length; i++) {
			dataString=String.valueOf(record_rss[i])+",";
			out.write(dataString.getBytes());
		}
		
		out.flush();  
        out.close(); 
        Toast.makeText(getApplicationContext(), "Data Saved to files",
        	     Toast.LENGTH_SHORT).show();
        
	}
	
	private void sendEmail() {
		// TODO Auto-generated method stub
		
		File file = new File(Environment.getExternalStorageDirectory(),"iBeacon"+".txt");
		Intent email = new Intent(android.content.Intent.ACTION_SEND);  

		String[] emailReciver = new String[]{"alexzhuqch@gmail.com"};//{"chen0832@e.ntu.edu.sg"};  
		  
		String  emailTitle = "Acceleration Data";  
		String emailContent = "Check attachment for the data";  

		email.putExtra(android.content.Intent.EXTRA_EMAIL, emailReciver);  

		email.putExtra(android.content.Intent.EXTRA_SUBJECT, emailTitle);  
		
		email.putExtra(android.content.Intent.EXTRA_TEXT, emailContent);  
		
		email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		email.setType("text/plain");

		startActivity(Intent.createChooser(email, "Send Email"));
		
	}
	
    private Handler mHandler = new Handler() {
    	public void handleMessage(Message msg){
    		//Date d = new Date();
    		//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    		//String t = sdf.format(d)+","+(String)msg.obj;
    		//String old = String.valueOf(tv.getText());
    		//current = old+"\n"+t;
    		tv.setText((String)msg.obj+","+String.valueOf(count)+","+String.valueOf(record_times));
    		tv.invalidate();
    	}
    };
}
