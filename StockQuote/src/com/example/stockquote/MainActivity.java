package com.example.stockquote;

/*
 * Brett Tolbert
 * CIS 4340
 * Stock Quote app
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	String stockSymbol = " ";
	
	//variables that will reference objects in the activity main layout
	EditText txtURL;
	Button btnLookUp;
	Button btnStop;
	TableLayout tblStockTable;
	//TextView txtVwStockName;
	//TextView txtVwStockPrice;
	TextView txtVwStockInfo;
	TextView txtStockName;
	TextView txtStockPrice;
	TextView txtNoJSONResults;
	TextView txtNumUpdates;
	
	Thread financeInfoThread; //the thread that will handle getting the stock quotes
	
	int updateCount = 0; //counter to record how many times the stock table has been updated
	boolean continueGettingUpdates = true; 
	
	//thread handler that will update the layout elements will the name
	//and price from the JSON string
	private Handler JSONHandler = new Handler(new Handler.Callback() {
		
	@Override	
	public boolean handleMessage(Message msg) {
		
		try {
			//tblStockTable.setVisibility(View.INVISIBLE); //hide table for when it's being updated
			
			//array is used to retrieve the name and price values that were stored in the JSON string
			String [] resultsArray = JSONParser(String.valueOf(msg.obj));
	
			//System.out.println("RESULTS " + resultsArray[0] + " " + resultsArray[1]);
			
			//check if there is information in the stock json string
			if((resultsArray[0] == "N/A") && (resultsArray[1] == "N/A")){
				txtNoJSONResults.setText("Sorry, but there is not any data for " +
			"the stock symbol you entered.");
				txtNoJSONResults.setVisibility(View.VISIBLE);
			}
			
			updateCount++;
			txtStockName.setText(resultsArray[0]);
			txtStockPrice.setText(resultsArray[1]);
			//System.out.println("***************** " + resultsArray[1]);
			txtNumUpdates.setText("Number of Updates: " + updateCount);
			
			//show the table that holds layout elements for the stock name and price
			tblStockTable.setVisibility(View.VISIBLE);
			btnStop.setVisibility(View.VISIBLE);
			
			Toast.makeText(getApplicationContext(), "Getting updated Stock values", Toast.LENGTH_SHORT).show();	 // let user know new stock values are being updated
			//tblStockTable.setVisibility(View.INVISIBLE);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 				
			return false;
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//storing reference to objects of the layout file
		txtURL = (EditText) findViewById(R.id.txtSymbol);
		btnLookUp = (Button) findViewById(R.id.btnLookUp);
		btnStop = (Button) findViewById(R.id.btnStop);
		
		tblStockTable = (TableLayout) findViewById(R.id.tableLayout1);
		txtStockName = (TextView) findViewById(R.id.txtStockName);
		txtStockPrice = (TextView) findViewById(R.id.txtStockPrice);
		txtNoJSONResults = (TextView) findViewById(R.id.txtNoContent);
		txtNumUpdates = (TextView) findViewById(R.id.txtUpdatesAmt);
		
		//hide the stock results table, change the color of some UI elements
		tblStockTable.setVisibility(View.INVISIBLE);
		btnStop.setVisibility(View.INVISIBLE);
		btnLookUp.setBackgroundColor(Color.WHITE);
		btnStop.setBackgroundColor(Color.WHITE);
		txtURL.setTextColor(Color.WHITE);		
		
		btnStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tblStockTable.setVisibility(View.INVISIBLE);
				btnStop.setVisibility(View.INVISIBLE);
				
				continueGettingUpdates = false;
				updateCount = 0;
				financeInfoThread.interrupt(); // stop the thread
			}
		});
		
		//
		btnLookUp.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//hide certain UI elements
				tblStockTable.setVisibility(View.INVISIBLE);
				txtNoJSONResults.setVisibility(View.INVISIBLE);
				
				continueGettingUpdates = true; //make sure updates will start
				updateCount = 0;
				
				stockSymbol = txtURL.getText().toString();
				
				//check if some text has been entered 
				if(stockSymbol.isEmpty()){
					Toast.makeText(getApplicationContext(), 
							"Enter the stock symbol", Toast.LENGTH_LONG).show();
				}
				else{
					//starting a new thread
					financeInfoThread = new Thread(){
						@Override
						public void run(){
							int refreshNumber = 0;
							//static URL that will be used to lookup the stock values
							while(continueGettingUpdates){
							
							String yahooURL = "http://finance.yahoo.com/webservice/v1/symbols/" + stockSymbol + "/quote?format=json";
							
							try {
								if(refreshNumber > 0){
								Thread.sleep(10000); //tell application to sleep every 10 seconds
								}
								URL browserURL = new URL(yahooURL);
								
								/*
								 * download the content of the specified URL to  a local string variable
								 */
								 BufferedReader in = 
										 new BufferedReader(new InputStreamReader(browserURL.openStream()));
								 
								 
								 String inputLine;
								 String outputLine = "";
								 
								 while ((inputLine = in.readLine()) != null){
									 outputLine = outputLine + inputLine + "\n";
								 }
								
								 in.close();
								 
								 Message msg = JSONHandler.obtainMessage();
								 msg.obj = outputLine;
								 JSONHandler.sendMessage(msg); //pass message from working thread to the handler 
															
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							refreshNumber++;
							}
						}
					};
				//start running the thread
				financeInfoThread.start();
					
				}
				
			}
		});
		
	}
	
	/*
	 * this method takes in a string and returns a string array that will hold the stock name and price
	 */
	public static String[] JSONParser(String JSON_DATA) throws JSONException{
		
	    String [] quoteArray = new String[2];
		
		//loop through the entire JSON string, retrieving JSON objects and JSON arrays until we get to the "field JSON object"
		JSONObject jObj = new JSONObject(JSON_DATA);
		JSONObject firstObj = jObj.getJSONObject("list");	
		
		JSONArray financeDataArray = firstObj.getJSONArray("resources");
		
		//System.out.println("****** LENGTH " + financeDataArray.length());
		
		//check if the array is empty
		if(financeDataArray.length() == 0){
			quoteArray[0] = "N/A";
			quoteArray[1] = "N/A";
		}else{
		
	    JSONObject quoteInfoObj = financeDataArray.getJSONObject(0);
	    
	    JSONObject resourceObj = quoteInfoObj.getJSONObject("resource");
	    
	    JSONObject fieldObj = resourceObj.getJSONObject("fields");
	    
	    //retrieve the stock name and price values
	    String stockName = fieldObj.getString("name");
	    String stockPrice = fieldObj.getString("price");
	    
	    //store the stock name and price values in the array that will be returned by the method
	    quoteArray[0] = stockName;
	    quoteArray[1] = stockPrice;
		}
	    
	    return quoteArray;

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
