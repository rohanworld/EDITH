package com.rohan.app.edith;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeText;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();

    TextToSpeech txtspch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        recyclerView = findViewById(R.id.recyclerView);
        welcomeText = findViewById(R.id.welcomeText);
        sendButton = findViewById(R.id.sendButton);
        messageEditText = findViewById(R.id.messageTextBox);
        messageList = new ArrayList<>();

        //Using texttospeech class and initalizing
        txtspch = new TextToSpeech(HomeActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    //You can set different Language here
                    txtspch.setLanguage(Locale.US);
//                    txtspch.isSpeaking()
                }
            }
        });


        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener(view -> {
            String question = messageEditText.getText().toString().trim();
            //Toast.makeText(HomeActivity.this, question, Toast.LENGTH_SHORT).show();
            addToChat(question, Message.SENT_BY_ME);
            callAPI(question);
            messageEditText.setText("");
            welcomeText.setVisibility(View.GONE);

            //hiding the keyboard
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(),0);
        });

        messageEditText.setOnTouchListener((v, event) -> {
            if (txtspch != null && txtspch.isSpeaking()) {
                txtspch.stop();
            }
            return false;
        });


    }

    void addToChat(String message, String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response, Message.SENT_BY_BOT);
        txtspch.speak(response, TextToSpeech.QUEUE_FLUSH, null);
    }



    //API KEY = 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
    void callAPI(String question){
        //okhttp
        messageList.add(new Message("Thinking...", Message.SENT_BY_BOT));
        JSONObject jsonBody = new JSONObject();
        try {
            //Below is for old chatgpt 3
//            jsonBody.put("model", "text-davinci-003");
//            jsonBody.put("prompt", question);
//            jsonBody.put("max_tokens", 4000);
//            jsonBody.put("temperature", 0);
            //below will eb for chatgpt 3.5 turbo

            jsonBody.put("model", "gpt-3.5-turbo");
            JSONArray messageArr = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("role", "user");
            obj.put("content", question);
            messageArr.put(obj);

            jsonBody.put("messages", messageArr);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
//                .header("Authorization", "Bearer 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")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to Answer your Query due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    try {
                        assert response.body() != null;
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
//                        String result = jsonArray.getJSONObject(0).getString("text");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    addResponse("Failed to Answer your Query due to "+response.body().toString());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        if(txtspch!=null){
            txtspch.stop();
            txtspch.shutdown();
        }
        super.onPause();
    }
}