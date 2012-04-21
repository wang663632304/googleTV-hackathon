package com.pheelicks.quiz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
  private static final String TAG = "MainActivity";

  private TextView mQuestionTextView;
  private List<Button> mOptionButtons;
  private List<Question> mQuestions;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.quiz);

    findUIElements();
    setupUI();
  }

  private void findUIElements()
  {
    mQuestionTextView = (TextView)findViewById(R.id.question_tv);
    mOptionButtons = new ArrayList<Button>(4);
    mOptionButtons.add((Button)findViewById(R.id.option_1));
    mOptionButtons.add((Button)findViewById(R.id.option_2));
    mOptionButtons.add((Button)findViewById(R.id.option_3));
    mOptionButtons.add((Button)findViewById(R.id.option_4));
  }

  private void setupUI()
  {
    mQuestions = loadQuestions();
    if(mQuestions == null)
    {
      Log.e(TAG, "Could not load quiz questions, exiting");
      finish();
      return;
    }

    Question firstQuestion = mQuestions.get(0);
    mQuestionTextView.setText(firstQuestion.title);
    mOptionButtons.get(0).setText(firstQuestion.correctAnswer);
    mOptionButtons.get(1).setText(firstQuestion.wrongAnswers.get(0));
    mOptionButtons.get(2).setText(firstQuestion.wrongAnswers.get(1));
    mOptionButtons.get(3).setText(firstQuestion.wrongAnswers.get(2));
  }

  private List<Question> loadQuestions()
  {
    try
    {
      List<Question> questions = QuizParser.load(this);
      return questions;
    }
    catch (XmlPullParserException e)
    {
      Log.e(TAG, "Couldn't parse quiz xml");
      Log.e(TAG, Log.getStackTraceString(e));
    }
    catch (IOException e)
    {
      Log.e(TAG, "IO error while parsing quiz xml");
      Log.e(TAG, Log.getStackTraceString(e));
    }

    return null;
  }
}