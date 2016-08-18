package ru.kodelnaya.dixifeedback;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;


import br.com.jansenfelipe.androidmask.MaskEditTextChangedListener;
import cz.msebera.android.httpclient.Consts;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import cz.msebera.android.httpclient.entity.mime.content.StringBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class FeedbackActivity extends Activity {

    protected JSONObject jObj;
    public SharedPreferences sp;
    protected RelativeLayout noInternet;

    boolean savedInfo = false;

    protected CheckBox accept;
    protected EditText name, surname, middlename, email, textFeed, phone;
    protected Spinner topic, region, punkt, address;
    protected int topicR, regionR, punktR, addressR = 0;
    protected Button attach, send, reconnect;

    protected String selectedFile = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //FontsOverride.replaceDefaultFont(this, "MONOSPACE", "fonts/font.ttf");
        setContentView(R.layout.activity_feedback);



        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        attach = (Button) findViewById(R.id.attach);

        final FilePickerDialog dialog = new FilePickerDialog(FeedbackActivity.this, properties);
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                selectedFile = files[0];
                if(!selectedFile.equals("")){
                    attach.setText("ФАЙЛ ПРИКРЕПЛЕН");
                }
                //files is the array of the paths of files selected by the Application User.
            }
        });

        sp = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        accept = (CheckBox) findViewById(R.id.accept);
        name = (EditText) findViewById(R.id.name);
        surname = (EditText) findViewById(R.id.surname);
        middlename = (EditText) findViewById(R.id.middlename);
        email = (EditText) findViewById(R.id.email);
        phone = (EditText) findViewById(R.id.phone);
        textFeed = (EditText) findViewById(R.id.text_feed);

        topic = (Spinner) findViewById(R.id.topic);
        region = (Spinner) findViewById(R.id.region);
        punkt = (Spinner) findViewById(R.id.punkt);
        address = (Spinner) findViewById(R.id.address);


        send = (Button) findViewById(R.id.send);

        noInternet = (RelativeLayout) findViewById(R.id.no_internet);
        noInternet.setVisibility(View.GONE);
        reconnect = (Button) findViewById(R.id.tryhard);

        TextView rules = (TextView)findViewById(R.id.rules);
        rules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dixy.ru/terms-of-use/"));
                startActivity(browserIntent);
            }
        });

        MaskEditTextChangedListener maskTEL = new MaskEditTextChangedListener("(###)###-##-##", phone);
        phone.addTextChangedListener(maskTEL);




        //protected EditText name, surname, middlename, email, textFeed, phone;
        //protected Spinner topic, region, punkt, address;



        name.setOnFocusChangeListener(focusListener);
        surname.setOnFocusChangeListener(focusListener);
        middlename.setOnFocusChangeListener(focusListener);
        email.setOnFocusChangeListener(focusListener);
        textFeed.setOnFocusChangeListener(focusListener);
        phone.setOnFocusChangeListener(focusListener);


        /*if(name.isFocused()){
            Log.i("TRUE", "");
            name.setBackground(getResources().getDrawable(R.drawable.popup_round_f));
        }*/

        /*phone.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });*/

        if (isNetworkAvailable()) {
            GetSpinnersTask GST = new GetSpinnersTask();
            GST.execute();
        }

        if (!isNetworkAvailable() && sp.getString("spinnerBase", "").equals("")) {
            //and spinners are empty
            noInternet.setVisibility(View.VISIBLE);

        } else {
            if (!sp.getString("spinnerBase", "").equals("")) {
                fillSpinners();
            }
        }

        /*sp.getString("last_name", ""),
                sp.getString("first_name", ""),
                sp.getString("parent_name", ""),
                sp.getString("email", ""),
                sp.getString("phone", ""),
                sp.getString("theme_id", ""),
                Integer.toString(0),
                Integer.toString(0),
                sp.getString("store_id", ""),
                sp.getString("content", "")});*/

        if(!sp.getString("last_name", "").equals("")){
            surname.setText(sp.getString("last_name", ""));
            name.setText(sp.getString("first_name", ""));
            middlename.setText(sp.getString("parent_name", ""));
            email.setText(sp.getString("email", ""));
            phone.setText(sp.getString("phone", ""));
        }

        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()) {
                    //and spinners are full
                    GetSpinnersTask GST = new GetSpinnersTask();
                    GST.execute();


                    noInternet.setVisibility(View.GONE);


                } else {
                    Toast.makeText(FeedbackActivity.this, "Эххх, все еще нет интернета :(", Toast.LENGTH_SHORT).show();
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()) {
                    if (surname.getText().toString().length() > 0 &&
                            name.getText().toString().length() > 0 &&
                            phone.getText().toString().length() > 0 &&
                            email.getText().toString().length() > 0 &&
                            textFeed.getText().toString().length() > 0
                            && topicR != 0 && topicR != -1
                            && regionR != 0 && regionR != -1
                            && punktR != 0 && punktR != -1
                            && addressR != 0 && addressR != -1) {

                        if (accept.isChecked()) {

                            FeedbackTask FT = new FeedbackTask();
                            FT.execute(new String[]{surname.getText().toString(),
                                    name.getText().toString(),
                                    middlename.getText().toString(),
                                    email.getText().toString(),
                                    phone.getText().toString().replaceAll("[\\+()\\- ]", ""),
                                    Integer.toString(topicR),
                                    Integer.toString(regionR),
                                    Integer.toString(punktR),
                                    Integer.toString(addressR),
                                    textFeed.getText().toString()});

                        } else {
                            Toast.makeText(FeedbackActivity.this, "Примите условия пользовательского соглашения", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(FeedbackActivity.this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
                    }


                } else {
                    /*
                    3. Send them when network became available
                     */

                    if (surname.getText().toString().length() > 0 &&
                            name.getText().toString().length() > 0 &&
                            phone.getText().toString().length() > 0 &&
                            email.getText().toString().length() > 0 &&
                            textFeed.getText().toString().length() > 0
                            && topicR != 0 && topicR != -1
                            && regionR != 0 && regionR != -1
                            && punktR != 0 && punktR != -1
                            && addressR != 0 && addressR != -1) {

                        if (accept.isChecked()) {

                            Toast.makeText(FeedbackActivity.this, "Сообщение будет отправлено при появлении сети", Toast.LENGTH_SHORT).show();

                            savedInfo = true;

                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("first_name", name.getText().toString());
                            editor.putString("last_name", surname.getText().toString());
                            editor.putString("parent_name", middlename.getText().toString());
                            editor.putString("email", email.getText().toString());
                            editor.putString("phone", phone.getText().toString().replaceAll("[\\+()\\- ]", ""));

                            editor.putString("theme_id", Integer.toString(topicR));
                            editor.putString("store_id", Integer.toString(addressR));

                            editor.putString("content", textFeed.getText().toString());
                            editor.putString("file", selectedFile);
                            editor.commit();

                            ClassExecutingTask executingTask = new ClassExecutingTask();
                            executingTask.start();


                        } else {
                            Toast.makeText(FeedbackActivity.this, "Примите условия пользовательского соглашения", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(FeedbackActivity.this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });


    }

    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                v.setBackground(getResources().getDrawable(R.drawable.popup_round_f));
            } else {
                v.setBackground(getResources().getDrawable(R.drawable.popup_round));
            }
        }
    };

    public class ClassExecutingTask {

        long delay = 10 * 1000; // delay in milliseconds
        LoopTask task = new LoopTask();
        Timer timer = new Timer("TaskName");

        public void start() {
            timer.cancel();
            timer = new Timer("TaskName");
            Date executionDate = new Date(); // no params = now
            timer.scheduleAtFixedRate(task, executionDate, delay);
        }

        private class LoopTask extends TimerTask {
            public void run() {
                if (isNetworkAvailable() && savedInfo) {
                    savedInfo = false;

                    FeedbackTask FT = new FeedbackTask();
                    FT.execute(new String[]{sp.getString("last_name", ""),
                            sp.getString("first_name", ""),
                            sp.getString("parent_name", ""),
                            sp.getString("email", ""),
                            sp.getString("phone", ""),
                            sp.getString("theme_id", ""),
                            Integer.toString(0),
                            Integer.toString(0),
                            sp.getString("store_id", ""),
                            sp.getString("content", "")});

                    Looper.prepare();
                    Toast.makeText(FeedbackActivity.this, "Последнее сохраненное сообщение отправлено", Toast.LENGTH_SHORT).show();

                }
            }
        }

    }

    private class GetSpinnersTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... v) {

            try {
                return sendGet("http://api.dixy.stageserver.pw/directory");
            } catch (Exception e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("t", result);

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("spinnerBase", result);
            editor.commit();

            fillSpinners();

        }
    }

    protected void fillSpinners() {

        String result = sp.getString("spinnerBase", "");
        JSONParser parser = new JSONParser();

        Object obj = null;
        try {
            obj = parser.parse(result);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        jObj = (JSONObject) obj;

        //REGIONS
        JSONArray arr = (JSONArray) jObj.get("regions");
        SpinnerItem[] regionsArr = new SpinnerItem[arr.size() + 1];

        SpinnerItem dummy = new SpinnerItem();
        dummy.title = "Регион";
        dummy.id = -1;

        regionsArr[0] = dummy;


        for (int i = 1; i < arr.size() + 1; i++) {
            JSONObject regionObj = (JSONObject) arr.get(i - 1);
            SpinnerItem item = new SpinnerItem();
            item.id = (Long) regionObj.get("id");
            item.title = (String) String.valueOf(regionObj.get("name"));
            regionsArr[i] = item;
        }
        SpinnerAdapter adapter = new SpinnerAdapter(FeedbackActivity.this, regionsArr);
        region.setAdapter(adapter);
        region.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                regionR = (int) (long) id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //END OF REGIONS


        //localities
        JSONArray arr2 = (JSONArray) jObj.get("localities");
        SpinnerItem[] cArr2 = new SpinnerItem[arr2.size() + 1];

        SpinnerItem dummy2 = new SpinnerItem();
        dummy2.title = "Населенный пункт";
        dummy2.id = -1;

        cArr2[0] = dummy2;


        for (int i = 1; i < arr2.size() + 1; i++) {
            JSONObject localitiesObj = (JSONObject) arr2.get(i - 1);
            SpinnerItem item = new SpinnerItem();
            item.id = (Long) localitiesObj.get("id");
            item.title = (String) localitiesObj.get("name");
            cArr2[i] = item;
        }
        SpinnerAdapter adapter2 = new SpinnerAdapter(FeedbackActivity.this, cArr2);
        punkt.setAdapter(adapter2);
        punkt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                punktR = (int) (long) id;

                if (punktR != 0 && punktR != -1) {
                    //STORES

                    Log.i("PUNKTR", Integer.toString(punktR));

                    int bad = 0;
                    JSONArray arr3 = (JSONArray) jObj.get("stores");
                    for (int i = 0; i < arr3.size(); i++) {
                        JSONObject curveObj = (JSONObject) arr3.get(i);
                        if (punktR != (int) (long) curveObj.get("locality_id")) {
                            bad++;
                        }
                    }


                    SpinnerItem[] cArr3 = new SpinnerItem[arr3.size() + 1 - bad];  //IT MUST BE 3 BUT ARR SIZE+1 IS 4
                    SpinnerItem dummy3 = new SpinnerItem();
                    dummy3.title = "Адрес";
                    dummy3.id = -1;

                    cArr3[0] = dummy3;
                    int added = 1;

                    for (int i = 1; i < arr3.size() + 1; i++) {
                        JSONObject curveObj = (JSONObject) arr3.get(i - 1);
                        if (punktR == (int) (long) curveObj.get("locality_id")) {
                            Log.i("HEY!", String.valueOf(curveObj.get("address")));
                            SpinnerItem item = new SpinnerItem();
                            item.id = (Long) curveObj.get("id");
                            item.title = (String) String.valueOf(curveObj.get("address"));
                            cArr3[added] = item;
                            added++;
                        }

                    }

                    SpinnerAdapter adapter3 = new SpinnerAdapter(FeedbackActivity.this, cArr3);
                    address.setAdapter(adapter3);
                    address.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view,
                                                   int position, long id) {
                            addressR = (int) (long) id;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> arg0) {
                        }
                    });
                    //END OF STORES

                } else {

                    SpinnerItem[] cArr3 = new SpinnerItem[1];
                    SpinnerItem dummy3 = new SpinnerItem();
                    dummy3.title = "Адрес";
                    dummy3.id = -1;
                    cArr3[0] = dummy3;

                    SpinnerAdapter adapter3 = new SpinnerAdapter(FeedbackActivity.this, cArr3);
                    address.setAdapter(adapter3);
                    address.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view,
                                                   int position, long id) {
                            addressR = (int) (long) id;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> arg0) {
                        }
                    });
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //END OF localities


        //THEMES
        JSONArray arr4 = (JSONArray) jObj.get("themes");
        SpinnerItem[] cArr4 = new SpinnerItem[arr4.size() + 1];

        SpinnerItem dummy4 = new SpinnerItem();
        dummy4.title = "Тема обращения";
        dummy4.id = -1;

        cArr4[0] = dummy4;

        for (int i = 1; i < arr4.size() + 1; i++) {
            JSONObject curveObj = (JSONObject) arr4.get(i - 1);
            SpinnerItem item = new SpinnerItem();
            item.id = (Long) curveObj.get("id");
            item.title = (String) curveObj.get("name");
            cArr4[(int) (long) curveObj.get("order")] = item;
        }
        SpinnerAdapter adapter4 = new SpinnerAdapter(FeedbackActivity.this, cArr4);
        topic.setAdapter(adapter4);
        topic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                topicR = (int) (long) id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //THEMES END

    }

    private class FeedbackTask extends AsyncTask<String[], Void, String[]> {

        @Override
        protected void onPreExecute()
        {
            Log.i("1234","start task");
            send.setActivated(false);
        }

        @Override
        protected String[] doInBackground(String[]... params) {
            try {
                ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
                String url = "http://api.dixy.stageserver.pw/feedback";

                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(url);

                MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();

                multipartEntity.addPart("last_name", new StringBody(params[0][0], contentType));
                multipartEntity.addPart("first_name", new StringBody(params[0][1], contentType));
                multipartEntity.addPart("parent_name", new StringBody(params[0][2], contentType));
                multipartEntity.addPart("email", new StringBody(params[0][3], contentType));
                multipartEntity.addPart("phone", new StringBody(params[0][4], contentType));
                multipartEntity.addPart("theme_id", new StringBody(params[0][5], contentType));
                multipartEntity.addPart("store_id", new StringBody(params[0][8], contentType));
                multipartEntity.addPart("content", new StringBody(params[0][9], contentType));

                if (!selectedFile.equals("") || !sp.getString("file", "").equals("")) {
                    multipartEntity.addPart("file", new FileBody(new File(selectedFile)));
                }

                post.setEntity(multipartEntity.build());

                HttpResponse response = client.execute(post);

                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                return new String[]{String.valueOf(response.getStatusLine().getStatusCode()), result.toString()};
            } catch (Exception e) {
                Log.i("ERROR:", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            Log.wtf("result", result[1]);
            // говно
            JSONParser parser = new JSONParser();

            Object obj = null;

            if (Integer.parseInt(result[0]) == 422) {
                try {
                    obj = parser.parse(result[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                JSONArray arr = (JSONArray) obj;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject cityObj = (JSONObject) arr.get(i);
                    sb.append((String) cityObj.get("message"));
                    sb.append("\n");
                }
                Toast.makeText(FeedbackActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            } else {

                if (Integer.parseInt(result[0]) == 404) {
                    Toast.makeText(FeedbackActivity.this, "Ошибка 404", Toast.LENGTH_SHORT).show();
                } else {

                    if (Integer.parseInt(result[0]) == 201) {




                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("first_name", name.getText().toString());
                        editor.putString("last_name", surname.getText().toString());
                        editor.putString("parent_name", middlename.getText().toString());
                        editor.putString("email", email.getText().toString());
                        editor.putString("phone", phone.getText().toString().replaceAll("[\\+()\\- ]", ""));

                        editor.putString("theme_id", Integer.toString(topicR));
                        editor.putString("store_id", Integer.toString(addressR));

                        editor.putString("content", textFeed.getText().toString());
                        editor.putString("file", "");
                        editor.commit();

                        name.setText("");
                        surname.setText("");
                        middlename.setText("");
                        email.setText("");

                        textFeed.setText("");
                        fillSpinners();

                        Toast.makeText(FeedbackActivity.this, "Сообщение успешно отправлено", Toast.LENGTH_SHORT).show();

                        send.setEnabled(true);

                    } else {
                        Toast.makeText(FeedbackActivity.this, "Неизвестная ошибка", Toast.LENGTH_SHORT).show();
                    }

                }
            }

        }
    }


    /* UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK *** UNIVERSAL CODE BLOCK */



    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String sendGet(String url) throws Exception {

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);


        HttpResponse response = client.execute(request);

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " +
                response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        Log.i("tt", response.toString());
        return result.toString();
    }

    private class SpinnerItem {
        public long id = 0;
        public String title = "";
    }

    private class SpinnerAdapter extends ArrayAdapter {

        private SpinnerItem[] items;

        public SpinnerAdapter(Context context, SpinnerItem[] items) {
            super(context, R.layout.spinner_item, R.id.spinner_text, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SpinnerItem item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.spinner_item, null);
            }
            ((TextView) convertView.findViewById(R.id.spinner_text))
                    .setText(item.title);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            SpinnerItem item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.spinner_item, null);
            }
            ((TextView) convertView.findViewById(R.id.spinner_text))
                    .setText(item.title);


            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public SpinnerItem getItem(int pos) {
            return items[pos];
        }

    }

}
