package co.gaptap;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by gareth on 7/20/13.
 */
public class WriteTagActivity  extends FragmentActivity
{

    IntentFilter[] intentFiltersArray = null;
    String[][] techListsArray;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    RequestQueue queue = null;
    View progressBar = null;
    int id = 0;
    Handler tagHandler = null;

    public static String EXTRA_NAME = "name";
    public static String EXTRA_PRICE = "price";
    public static String EXTRA_DESC = "desc";
    public static String EXTRA_ID = "tag_id";

    public static int ACTIVITY_RETURN_SUCCESS = 2013;
    public static int ACTIVITY_RETURN_FAIL = 2099;

    ImageView progressImage = null;
    TextView progressText = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_writetag);


        id = getIntent().getIntExtra(WriteTagActivity.EXTRA_ID,0);

        progressImage = ((ImageView) findViewById(R.id.progressImage));
        progressText = ((TextView) findViewById(R.id.status));

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try
        {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                               You should specify only the ones that you need. */
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new RuntimeException("fail", e);
        }
        IntentFilter td = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        intentFiltersArray = new IntentFilter[] {ndef, td};

        techListsArray = new String[][] { new String[] { NfcF.class.getName(),NfcA.class.getName(),Ndef.class.getName(), NdefFormatable.class.getName() } };

        queue = Volley.newRequestQueue(this);

        tagHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                Toast.makeText(WriteTagActivity.this,msg.getData().getString("msg"),Toast.LENGTH_LONG).show();
            }
        };

        progressImage.setBackgroundResource(R.drawable.writetag_ready);
        progressText.setText("Ready for first tag!");
    }


    private void WriteTag(final Tag tagFromIntent)
    {
        progressImage.setBackgroundResource(R.drawable.writetag_working);
        progressText.setText("Writing to tag...");
        Log.e("Tag", "In tag land");

        ((Thread) new Thread(){

            public void run()
            {

                NdefRecord aaRecord = NdefRecord.createApplicationRecord("co.gaptap");
                NdefRecord idRecord = NdefRecord.createMime("text/plain", Integer.toString(id).getBytes(Charset.forName("US-ASCII")));
                NdefMessage tagMsg = new NdefMessage(new NdefRecord[]{idRecord,aaRecord});


                Message msg = new Message();
                Bundle bundle = new Bundle();

                //This could go all kinds of weird
                Ndef thisNdef = null;


                try
                {
                    thisNdef = Ndef.get(tagFromIntent);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                if(null == thisNdef)
                {
                    NdefFormatable formatter = NdefFormatable.get(tagFromIntent);
                    try
                    {
                        formatter.connect();
                        //formatter.format( new NdefMessage(new NdefRecord[]{NdefRecord.createApplicationRecord("co.gaptap")}));
                        formatter.format(tagMsg);
                        formatter.close();

                        bundle.putString("msg","Tag successfully written!");
                        msg.setData(bundle);
                        tagHandler.sendMessage(msg);


                        Intent in = new Intent();
                        setResult(WriteTagActivity.ACTIVITY_RETURN_SUCCESS,in);
                        finish();
                        return;
                    }
                    catch(Exception d)
                    {
                        d.printStackTrace();

                        bundle.putString("msg","There was an error formating the tag");
                        msg.setData(bundle);
                        tagHandler.sendMessage(msg);
                    }
                }
                else
                {
                    try
                    {
                        if(null == thisNdef)
                        {
                            throw new FormatException("No NDEF Tag returned from get");
                        }
                        else
                        {
                            thisNdef.connect();
                        }

                        if(thisNdef.isWritable())
                        {

                            Log.i("WriteTag Size Check", "Wrote " + tagMsg.getByteArrayLength());
                            thisNdef.writeNdefMessage(tagMsg);
                            thisNdef.close();

                            //Success
                            bundle.putString("msg","Tag successfully written!");
                            msg.setData(bundle);
                            tagHandler.sendMessage(msg);


                            Intent in = new Intent();
                            setResult(WriteTagActivity.ACTIVITY_RETURN_SUCCESS,in);
                            finish();
                            return;
                        }
                        else
                        {
                            bundle.putString("msg","Tag was readonly");
                            msg.setData(bundle);
                            tagHandler.sendMessage(msg);
                        }


                    }
                    catch (IOException e)
                    {
                        bundle.putString("msg","An unspecified IO exception occured");
                        msg.setData(bundle);
                        tagHandler.sendMessage(msg);
                    }
                    catch (FormatException e)
                    {
                        e.printStackTrace();
                        bundle.putString("msg","There was an error formatting the tag");
                        msg.setData(bundle);
                        tagHandler.sendMessage(msg);
                    }

                    Intent in = new Intent();
                    setResult(WriteTagActivity.ACTIVITY_RETURN_FAIL,in);
                    finish();
                }
            }
        }).start();
    }

    public void onPause()
    {
        super.onPause();
        Log.e("Pausing","Pausing");
        if(mAdapter != null)
            mAdapter.disableForegroundDispatch(this);
    }

    public void onResume()
    {
        super.onResume();

        if(mAdapter != null)
            mAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    public void onNewIntent(Intent intent)
    {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        WriteTag(tagFromIntent);
    }
}
