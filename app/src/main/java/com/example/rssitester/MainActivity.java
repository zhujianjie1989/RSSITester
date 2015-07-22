package com.example.rssitester;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class MainActivity extends Activity implements LeScanCallback {
    private static final String TAG = "BluetoothGattActivity";
	private BluetoothAdapter mBluetoothAdapter;
	private TextView tv;
	private String current;
	private AlertDialog ad;

	int count = 0;
	int[] record_rss = new int [7000]; //max 1000
	int record_times = 1;

	DatagramSocket socket;
	InetAddress serverAddress;

	public void initsocket(){
		try {
			EditText ip = (EditText)findViewById(R.id.ET_IP);
			EditText port = (EditText)findViewById(R.id.ET_Port);

			if (socket!=null)
				socket.close();

			socket=new   DatagramSocket (Integer.parseInt(port.getText().toString()));
			serverAddress = InetAddress.getByName(ip.getText().toString());

		} catch (IOException e) {
			e.printStackTrace();
		}
		//new Toast(this).makeText(this, "initsocket success ", Toast.LENGTH_LONG).show();

	}

	private int getCount = 50;
	public synchronized void sendMsg(final String msg){

		new Thread(new Runnable() {
			@Override
			public void run() {
				count++;
				byte data[] = msg.getBytes();
				DatagramPacket  packa = new DatagramPacket(data , data.length , serverAddress , 60000);
				try {
					socket.send(packa);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv = (TextView) findViewById(R.id.bName);
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
        ad = new AlertDialog.Builder(this).create();
        ad.setMessage("Scan Complete");
        ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
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
				sendMsg("-10000");
            	stopScan();
				socket.close();
				socket=null;
				serverAddress = null;
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


		if (socket==null)
		{
			initsocket();
		}
		EditText num = (EditText)findViewById(R.id.ET_NUM);
		getCount = Integer.parseInt(num.getText().toString());

		switer=true;
		count = 0;
		handler.sendEmptyMessage(0);
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

	
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

		if (count > getCount){
			sendMsg("-1000");
			stopScan();
			return;
		}
		String str = device.getAddress();
		String substring = str.substring(Math.max(str.length() - 5, 0));

		//Log.i(TAG, "New LE Device: " + device.getAddress() + " @ " + rssi);
		EditText mac = (EditText)findViewById(R.id.MAC);
		if (substring.equals(mac.getText().toString())) {
			String textTochange = String.valueOf(rssi);
			sendMsg(textTochange);
			Log.e("rssi", "" + rssi + " " + substring);
			Message msg = new Message();
			msg.obj=substring;
			mHandler.sendMessage(msg);

		}

		
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
