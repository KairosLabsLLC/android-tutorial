package com.example.tutorial.tutorial_4;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.widget.EditText;
import android.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.globalcollect.gateway.sdk.client.android.sdk.communicate.C2sCommunicatorConfiguration;
import com.globalcollect.gateway.sdk.client.android.sdk.model.AmountOfMoney;
import com.globalcollect.gateway.sdk.client.android.sdk.model.CountryCode;
import com.globalcollect.gateway.sdk.client.android.sdk.model.CurrencyCode;
import com.globalcollect.gateway.sdk.client.android.sdk.model.Environment.EnvironmentType;
import com.globalcollect.gateway.sdk.client.android.sdk.model.PaymentContext;
import com.globalcollect.gateway.sdk.client.android.sdk.model.PreparedPaymentRequest;
import com.globalcollect.gateway.sdk.client.android.sdk.model.Region;
import com.globalcollect.gateway.sdk.client.android.sdk.model.iin.IinDetailsResponse;
import com.globalcollect.gateway.sdk.client.android.sdk.model.iin.IinStatus;
import com.globalcollect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProduct;
import com.globalcollect.gateway.sdk.client.android.sdk.session.GcSession;
import com.globalcollect.gateway.sdk.client.android.sdk.model.PaymentRequest;
import com.globalcollect.gateway.sdk.client.android.sdk.asynctask.IinLookupAsyncTask.OnIinLookupCompleteListener;
import com.globalcollect.gateway.sdk.client.android.sdk.asynctask.PaymentProductAsyncTask.OnPaymentProductCallCompleteListener;
import com.globalcollect.gateway.sdk.client.android.sdk.session.GcSessionEncryptionHelper.OnPaymentRequestPreparedListener;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final String sessionUrl = "http://10.0.2.2:3000/api/ingenico-session ";
    private static final String paymentUrl = "http://10.0.2.2:3000/api/ingenico-encrypted ";
    private Context androidContext;
    private GcSession session;
    private String paymentProductId;
    private PaymentRequest paymentRequest = new PaymentRequest();
    private RequestQueue requestQueue;
    private String cardNumber;

    // Payment Context
    private Long amountValue = 3000L;
    private CurrencyCode currencyCode = CurrencyCode.EUR;
    private AmountOfMoney amountOfMoney = new AmountOfMoney(amountValue, currencyCode);
    private CountryCode countryCode  = CountryCode.FR;
    private Boolean isRecurring  = false;
    private PaymentContext paymentContext = new PaymentContext(amountOfMoney, countryCode, isRecurring);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);
        androidContext = this;


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, sessionUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("onResponse", response.toString());
                        try {
                            String clientSessionId = response.getJSONObject("body").getString("clientSessionId");
                            String customerId = response.getJSONObject("body").getString("customerId");
                            Region region = Region.valueOf(response.getJSONObject("body").getString("region"));
                            EnvironmentType environment = EnvironmentType.Sandbox;
                            String applicationIdentifier = "Tutorial/v4";

                            session = C2sCommunicatorConfiguration.initWithClientSessionId(
                                    clientSessionId,
                                    customerId,
                                    region,
                                    environment,
                                    applicationIdentifier
                            );
                        } catch (JSONException e) {

                            Log.e("JSONException - session", e.getLocalizedMessage());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", System.getProperty("http.agent"));
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void submitCard(View view) {
        EditText textCreditCardNumber = (EditText) findViewById(R.id.editTextCreditCardNumber);
        cardNumber = textCreditCardNumber.getText().toString();
        Log.d("cardNumber", cardNumber);

        EditText textCvv = (EditText) findViewById(R.id.editTextCvv);
        String cvv = textCvv.getText().toString();
        Log.d("cvv", cvv);

        EditText textExpiryDate = (EditText) findViewById(R.id.editTextExpiryDate);
        String expiryDate = textExpiryDate.getText().toString();
        Log.d("expiryDate", expiryDate);


        paymentRequest.setTokenize(false);

        paymentRequest.setValue("cardNumber", cardNumber);
        paymentRequest.setValue("cvv", cvv);
        paymentRequest.setValue("expiryDate", expiryDate);

        doIinLookup();


    }

    public void doIinLookup() {
        OnIinLookupCompleteListener listener = new OnIinLookupCompleteListener() {

            @Override
            public void onIinLookupComplete(IinDetailsResponse response) {
                Log.d("Payment Product response", response.getClass().toString());
                response.setStatus(IinStatus.SUPPORTED);
                Log.d("Payment Product allowed", String.valueOf(response.isAllowedInContext()));
                Log.d("Payment Product status", String.valueOf(response.getStatus()));
                Log.d("Payment Product country", String.valueOf(response.getCountryCode()));
                Log.d("Payment Product id", String.valueOf(response.getPaymentProductId()));

                paymentProductId = String.valueOf(response.getPaymentProductId());
                retrievePaymentProduct();
            }
        };
        // String unmasked = paymentRequest.getUnmaskedValue("cardNumber", "1234 5678 9");

        // session.getIinDetails(androidContext, cardNumber.substring(0,6), listener, paymentContext);
        session.getIinDetails(androidContext, cardNumber, listener, paymentContext);

    }


    public void retrievePaymentProduct() {
        // Log.d("Payment Product id", paymentProductId);
        OnPaymentProductCallCompleteListener listener = new OnPaymentProductCallCompleteListener() {

            @Override public void onPaymentProductCallComplete(PaymentProduct paymentProduct) {
                Log.d("Payment Product", paymentProduct.toString());
                if (paymentProduct == null) {
                    Log.e("onPaymentProductCallComplete - ", "An error occured while getting the payment product.");
                } else {
                    paymentRequest.setPaymentProduct(paymentProduct);
                    encryptRequest();
                }
            }
        };
        // Log.d("Payment Product id", paymentProductId);
        // session.getPaymentProduct(androidContext, paymentProductId, paymentContext, listener);
        session.getPaymentProduct(androidContext, paymentProductId, paymentContext, listener);

    }


    public void encryptRequest() {
        OnPaymentRequestPreparedListener listener = new OnPaymentRequestPreparedListener() {

            @Override
            public void onPaymentRequestPrepared(PreparedPaymentRequest preparedPaymentRequest) {
                if (preparedPaymentRequest == null ||
                        preparedPaymentRequest.getEncryptedFields() == null) {
                    Log.e("onPaymentRequestPrepared - ", "An error occured while encrypting the request.");

                } else {

                    try {
                        Log.d("Encrypted", preparedPaymentRequest.getEncryptedFields());
                        JSONObject jsonParams = new JSONObject();
                        JSONObject order = new JSONObject();
                        JSONObject amountOfMoney = new JSONObject();


                        jsonParams.put("encryptedCustomerInput", preparedPaymentRequest.getEncryptedFields());
                        amountOfMoney.put("currencyCode", currencyCode);
                        amountOfMoney.put("amount", amountValue);
                        order.put("amountOfMoney", amountOfMoney);
                        jsonParams.put("order", order);

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                                Request.Method.POST, paymentUrl, jsonParams,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("Payment response", response.toString());

                                        AlertDialog.Builder builder = new AlertDialog.Builder(androidContext);
                                        builder.setMessage("The payment was suddenly sent!");
                                        builder.setCancelable(true);
                                        AlertDialog alert4 = builder.create();
                                        alert4.show();
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.e("onPaymentRequestPrepared - ", error.getLocalizedMessage());
                                    }
                                }){
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                HashMap<String, String> headers = new HashMap<String, String>();
                                headers.put("Content-Type", "application/json; charset=utf-8");
                                headers.put("User-agent", System.getProperty("http.agent"));
                                return headers;
                            }
                        };

                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        Log.e("onPaymentRequestPrepared - JSONException", e.getLocalizedMessage());
                    }

                }
            }
        };

        session.preparePaymentRequest(paymentRequest, androidContext, listener);
    }
}
