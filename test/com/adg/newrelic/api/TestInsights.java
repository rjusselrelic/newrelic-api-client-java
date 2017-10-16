package com.adg.newrelic.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TestInsights {

	// Used to ensure the async work has completed
	private CountDownLatch lock = new CountDownLatch(1);
	private Long lCountAsync;
	
	// API keys we'll use for the tests
	private APIKeyset keys;
	
	public static final String NRQL_QUERY = "SELECT count(*) FROM Transaction";
	public static final long TIMEOUT = 10000;
	
	@Before
	public void setUp() throws Exception {
		
		// Read in the config files
		Config conf = ConfigFactory.load();
		
		// Get the first config from the array
		List<String> configArr = conf.getStringList("newrelic-api-lib.configArr");
		String configId = "newrelic-api-lib." + configArr.get(0);
		
		// Create the API Keyset from the config file
		keys = new APIKeyset();
		String accountId = conf.getString(configId + ".accountId");
		keys.setAccountId(accountId);
		String insightsQueryKey = conf.getString(configId + ".insightsQueryKey");
		keys.setInsightsQueryKey(insightsQueryKey);
		System.out.println("Insights Test using keyset for account: " + keys.getAccountId());
		
	}

	@Test
	public void testQuerySync() throws IOException {
		Response rsp = Insights.querySync(keys, NRQL_QUERY);
		
		// Convert the response into JSON and pull out the count
		JSONObject jResponse = new JSONObject(rsp.body().string());
		JSONArray jResults = jResponse.getJSONArray("results");
		Long lCount = jResults.getJSONObject(0).getLong("count");
		assertNotNull(lCount);
		System.out.println("[Sync] count is: " + lCount.toString());
	}

	@Test
	public void testQueryAsync() throws IOException, InterruptedException {
		
		// Call the async version of the API
		Insights.queryAsync(keys, NRQL_QUERY, new Callback() {

			@Override
			public void onFailure(Call call, IOException e) {
				assertFalse(e.getMessage(), true);
			}

			@Override
			public void onResponse(Call call, Response rsp) throws IOException {
				
				// Convert the response into JSON and pull out the count
				JSONObject jResponse = new JSONObject(rsp.body().string());
				JSONArray jResults = jResponse.getJSONArray("results");
				lCountAsync = jResults.getJSONObject(0).getLong("count");
				assertNotNull(lCountAsync);
				System.out.println("[Async] count is: " + lCountAsync.toString());
				
				// Tell the lock this value has been returned
				lock.countDown();
			}
		});
		
		// Wait for the lock to count down from the callback
		lock.await(TIMEOUT, TimeUnit.MILLISECONDS);
		assertNotNull(lCountAsync);
		
	}

}
