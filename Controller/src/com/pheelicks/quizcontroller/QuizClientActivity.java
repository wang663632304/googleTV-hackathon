package com.pheelicks.quizcontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class QuizClientActivity extends Activity {
  private static final String TAG = "QuizClientActivity";

  private static final String SERVER_IP = "192.168.51.177";

  private boolean connected = false;
  private static final int SERVERPORT = 13337;
  private static final int CLIENT_ID = 1; // TODO do not hard code
  private PrintWriter mOutWriter;
  private BufferedReader mInputReader;

  private Button mConnectButton;
  private TextView mQuestionTextView;
  private Button mAnswer1Button;
  private Button mAnswer2Button;
  private Button mAnswer3Button;
  private Button mAnswer4Button;

  private Question mCurrentQuestion;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.quizclient);

    Log.i(TAG, "Started client " + CLIENT_ID);

    // Find views
    mConnectButton = (Button)findViewById(R.id.connect_btn);
    mQuestionTextView = (TextView)findViewById(R.id.Question);
    mAnswer1Button = (Button)findViewById(R.id.Answer1);
    mAnswer2Button = (Button)findViewById(R.id.Answer2);
    mAnswer3Button = (Button)findViewById(R.id.Answer3);
    mAnswer4Button = (Button)findViewById(R.id.Answer4);
  }

  public void connectPressed(View view)
  {
    if (!connected) {
      Thread cThread = new Thread(new ClientThread(CLIENT_ID));
      cThread.start();
    }
  }

  // Will get called when the connection state to the server changes
  public void setConnected(final boolean connected)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        if(connected)
        {
          mConnectButton.setVisibility(View.GONE);
        }
        else
        {
          mConnectButton.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  private void updateWithQuestion(Question question)
  {
    mCurrentQuestion = question;
    mQuestionTextView.setText(question.title);
    mAnswer1Button.setText(question.answers.get(0));
    mAnswer2Button.setText(question.answers.get(1));
    mAnswer3Button.setText(question.answers.get(2));
    mAnswer4Button.setText(question.answers.get(3));
  }

  public void answerPressed(View v)
  {
    String chosenAnswer = (String)((Button)v).getText();
    mCurrentQuestion.correctAnswer = chosenAnswer;
    sendMessageToServer(CLIENT_ID, JSONMessages.postAnswer(mCurrentQuestion));
  }

  // Use methods here to send/receive message to/from server
  public void sendMessageToServer(int client, JSONObject message)
  {
    if(mOutWriter != null)
    {
      try
      {
        message.put(JSONAPI.CLIENT, client);
      }
      catch (JSONException e)
      {
        Log.e(TAG, Log.getStackTraceString(e));
      }
      String stringified = message.toString();
      Log.d(TAG, "Sending to server:" + stringified);
      mOutWriter.println(stringified);
    }
  }

  public void receivedMessageFromServer(int client, JSONObject message)
  {
    Log.d(TAG, "Received message from server: " + message.toString());

    try
    {
      String msgType = message.getString(JSONAPI.MSG_TYPE);

      if(JSONAPI.NEW_QUESTION.equalsIgnoreCase(msgType))
      {
        Log.d(TAG, "Got new question");
        Question q = new Question(message.getJSONObject(JSONAPI.MSG_VALUE));
        updateWithQuestion(q);
      }
      else
      {
        Log.w(TAG, "Received unknown message type from server: " + msgType);
      }

    }
    catch (JSONException e)
    {
      Log.e(TAG, Log.getStackTraceString(e));
    }

  }

  public class ClientThread implements Runnable {
    private int mClient;

    public ClientThread(int client)
    {
      mClient = client;
    }

    @Override
    public void run() {
      Socket socket = null;
      try {
        InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
        Log.d(TAG, "Connecting client " + mClient + "...");
        socket = new Socket(serverAddr, SERVERPORT + mClient);
        Log.i(TAG, "Connected to server on port: " + (SERVERPORT + mClient));

        // Hide connect button
        connected = true;
        setConnected(connected);

        mOutWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                                                                               .getOutputStream())), true);
        mInputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        while (connected) {
          try {
            String st = mInputReader.readLine();
            if(st != null)
            {
              final JSONObject json = new JSONObject(st);
              runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  receivedMessageFromServer(mClient, json);
                }
              });
            }
          } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
          }
        }
      }
      catch (Exception e)
      {
        Log.d(TAG, "Error with socket on client " + mClient);
        Log.e(TAG, Log.getStackTraceString(e));
        connected = false;
        setConnected(connected);
      }
      finally
      {
        if(socket != null && !socket.isClosed())
        {
          try
          {
            socket.close();
          }
          catch (IOException e)
          {
            Log.e(TAG, Log.getStackTraceString(e));
          }
          Log.d(TAG, "Closed client " + mClient + " socket");
        }
      }
    }
  }
}