package com.google.hany.taskmaker;

/**
 * Created by Hany on 11-02-2018.
 */

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class fabTest {


    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(MainActivity.class);

    @Test
    public void triggerIntentTest() {
       //checking click with id of fab button
        onView(withId(R.id.fab)).perform(click());
        //checking desired class name
        intended(hasComponent(AddTaskActivity.class.getName()));

    }
}
