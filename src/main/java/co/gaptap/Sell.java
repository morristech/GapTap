package co.gaptap;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

/**
 * Created by gareth on 7/20/13.
 */
public class Sell extends Fragment
{
    IntentFilter[] intentFiltersArray = null;
    String[][] techListsArray;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    RequestQueue queue = null;
    View progressBar = null;

    EditText Name = null;
    EditText Price = null;
    EditText Desc = null;
    int lastId = 0;

    ProgressDialog dialog = null;

    public static int RESULT = 9345;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        queue = Volley.newRequestQueue(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_sell,	container, false);

        Name = ((EditText) rootView.findViewById(R.id.itemName));
        Price = ((EditText) rootView.findViewById(R.id.itemPrice));
        Desc = ((EditText) rootView.findViewById(R.id.itemDesc));
        progressBar = rootView.findViewById(R.id.progressBar);

        ((Button) rootView.findViewById(R.id.startWritingButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                if(Name.getText().toString().equals("") || Price.getText().toString().equals(""))
                {
                    Toast.makeText(getActivity(),"An item needs a name and a price before it can be can be written to a tag!",Toast.LENGTH_LONG).show();
                }
                else
                {
                    progressBar.setVisibility(View.VISIBLE);

                    dialog = new ProgressDialog(getActivity());
                    dialog.setMessage("Contacting the GapTap Fog\r\nPreparing your inventory\r\nPlease wait....");
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            queue.cancelAll(getActivity());
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    dialog.show();

                    JSONObject post = new JSONObject();
                    try
                    {
                        post.put("name", Name.getText().toString());
                        post.put("price", Price.getText().toString());
                        post.put("desc", Desc.getText().toString());
                        post.put("deviceid", Settings.Secure.getString(getActivity().getContentResolver(),Settings.Secure.ANDROID_ID));
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "An error Was encountered preparing to connect to the GapTap fog.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, "https://gaptap.co/api/additem.php", post, new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response)
                        {
                            dialog.dismiss();
                            progressBar.setVisibility(View.GONE);
                            Log.e("response", response.toString());
                            try
                            {
                                if(response.has("success") && response.getBoolean("success"))
                                {
                                    lastId = response.getInt("id");
                                    Intent writeIntent = new Intent(getActivity(), WriteTagActivity.class);
                                    writeIntent.putExtra(WriteTagActivity.EXTRA_NAME,Name.getText().toString());
                                    writeIntent.putExtra(WriteTagActivity.EXTRA_PRICE,Price.getText().toString());
                                    writeIntent.putExtra(WriteTagActivity.EXTRA_DESC,Desc.getText().toString());
                                    writeIntent.putExtra(WriteTagActivity.EXTRA_ID,lastId);

                                    startActivityForResult(writeIntent,RESULT);
                                }
                                else
                                {
                                    Toast.makeText(getActivity(), "An Error Was encountered processing the GapTap fog.\n\nYour item couldn't be added.", Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener()
                    {

                        @Override
                        public void onErrorResponse(VolleyError error)
                        {
                            dialog.dismiss();
                            progressBar.setVisibility(View.GONE);

                            try
                            {
                                Log.e("Error",error.getMessage().toString());
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            Toast.makeText(getActivity(), "An Error Was encountered connecting to the GapTap fog.\n\nYour item couldn't be added.", Toast.LENGTH_LONG).show();
                        }
                    });

                    queue.add(jsObjRequest);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == Sell.RESULT)
        {
            if(resultCode == WriteTagActivity.ACTIVITY_RETURN_FAIL)
            {
                if(lastId != 0)
                {
                    JSONObject post = new JSONObject();
                    try
                    {
                        post.put("id", Name.getText().toString());
                        post.put("deviceid", Settings.Secure.getString(getActivity().getContentResolver(),Settings.Secure.ANDROID_ID));
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "An error Was encountered preparing to connect to the GapTap fog.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, "https://gaptap.co/api/removeitem.php", post, new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response)
                        {
                            dialog.dismiss();
                            progressBar.setVisibility(View.GONE);
                            Log.e("response", response.toString());
                            try
                            {
                                if(response.has("success") && response.getBoolean("success"))
                                {
                                    //Alls good
                                }
                                else
                                {
                                    Toast.makeText(getActivity(), "An Error Was encountered processing the GapTap fog.\n\nYour item couldn't be removed. Please try later.", Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener()
                    {

                        @Override
                        public void onErrorResponse(VolleyError error)
                        {
                            dialog.dismiss();
                            progressBar.setVisibility(View.GONE);

                            try
                            {
                                Log.e("Error",error.getMessage().toString());
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            Toast.makeText(getActivity(), "An Error Was encountered connecting to the GapTap fog.\n\nYour item couldn't be removed.", Toast.LENGTH_LONG).show();
                        }
                    });

                    queue.add(jsObjRequest);
                }
            }
            else
            {
                Name.setText("");
                Price.setText("");
                Desc.setText("");
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}
