package com.genexuanqi.spaceAgainstSpontaneity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity {

    // ======================== START OF GLOBAL VARIABLES ====================================== //
    TextView textView_question;
    TextView textView_timer;
    TextView textView_score;
    TextView textView_round;
    TextView textView_playerName;
    public static TextView textView_annoucement;
    public static TextView textView_area;
    View decorView; // for hiding of status and navigation bars

    // for displaying of player-info
    public static ArrayList<String> playerList; // a list with all player names

    // for displaying of player Hand info
    ArrayList<String> hand;
    public static RecyclerView recyclerViewHand;
    RecyclerviewAdaptor recyclerviewAdaptorHand;

    ConstraintLayout layout_submit; // the layout where the dragged card is dropped

    // for displaying of playArea
    ArrayList<String> playArea;
    public static RecyclerView recyclerViewPlayArea;
    RecyclerviewAdaptor recyclerviewAdapterPlayArea;

    // for storing of player-score info
    public static HashMap<String, Integer> scoreMap = new HashMap<>();

    // for displaying of winner info
    CardView cardview_winning;
    TextView textView_winningAns;
    TextView textView_winnerName;

    // for the banner of battefield/yourcards above recyclerview
    ImageView imageView_hands;
    ImageView imageView_battlefield;

    // for banner display of captain/crew
    ImageView imageView_captain;
    ImageView imageView_crew;

    Socket socket;
    public static boolean isVoter; // boolean to determine if the player is a voter or an answerer. Will change depending on info received by socket.
    public static boolean canAnswer; // if true, means answerers still got time to pick an answer from their hand. else, no more time to pick an answer from hand.
    public static boolean canVote;
    public static int score = 0;
    public static int round = 1;

    // ======================== END OF GLOBAL VARIABLES ====================================== //

    // ======================== START OF ONCREATE FUNCTION ====================================== //

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == 0)
                    decorView.setSystemUiVisibility(hideSystemBars());
            }
        });

        textView_question = findViewById(R.id.textView_question); // initialise question box textView
        textView_timer = findViewById(R.id.textView_timer); // initialise timer textView
        textView_score = findViewById(R.id.textView_score);// initialise score textView
        textView_score.setText(Integer.toString(score));
        textView_round = findViewById(R.id.textView_round);// initialise round textView
        textView_round.setText(Integer.toString(round));
        textView_annoucement = findViewById(R.id.textView_Annoucement); // initialise the annoucement textView
        textView_playerName = findViewById(R.id.textView_playerName); // display of player name
        textView_playerName.setText(JoinGame.userName);

        // initialise display of winning card
        cardview_winning = findViewById(R.id.cardview_winning);
        textView_winningAns = findViewById(R.id.textView_winningAns);
        textView_winnerName = findViewById(R.id.textView_winnerName);
        cardview_winning.setVisibility(View.INVISIBLE);

        playerList = new ArrayList<>(); // initialise the player name arraylist

        imageView_battlefield = findViewById(R.id.imageView_battlefield);
        imageView_hands = findViewById(R.id.imageView_hands);

        imageView_crew = findViewById(R.id.imageView_crew);
        imageView_captain = findViewById(R.id.imageView_captain);

        // Display hand in client
        hand = new ArrayList<>();
        recyclerViewHand = findViewById(R.id.recyclerView_hands);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewHand.setLayoutManager(layoutManager);
        recyclerviewAdaptorHand = new RecyclerviewAdaptor(this, hand);
        recyclerViewHand.setAdapter(recyclerviewAdaptorHand);
        recyclerViewHand.setHasFixedSize(true);
        recyclerViewHand.setItemViewCacheSize(20);
        recyclerViewHand.setDrawingCacheEnabled(true);
        recyclerViewHand.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        layout_submit = findViewById(R.id.layout_submit);
        layout_submit.setOnDragListener(new DragListener());

        // Display play area in client
        playArea = new ArrayList<>();
        recyclerViewPlayArea = findViewById(R.id.recyclerView_playArea);
        LinearLayoutManager layoutManagerArea = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewPlayArea.setLayoutManager(layoutManagerArea);
        recyclerviewAdapterPlayArea = new RecyclerviewAdaptor(this, playArea);
        recyclerViewPlayArea.setAdapter(recyclerviewAdapterPlayArea);

        isVoter = false; // initialise as an answerer until client is assigned as a voter
        canAnswer = false; // client still has time to be able to choose the answer card

        // initialise the quit button. To allow players to quit the game.
        ImageView quitGameButton = (ImageView) findViewById(R.id.bttn_quit);
        quitGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Quit Game")
                        .setMessage("Are you sure you want to quit the game?")
                        .setPositiveButton("Yes, Quit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                onBackPressed();
                            } // triggers the back button when user wants to quit
                        })
                        .setNegativeButton("No, Stay", null) // null listener returns back to game if user clicks no
                        .show();
            }
        });

        try {
            socket = JoinGame.socket;
            runGame();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage() + "", Toast.LENGTH_SHORT).show();
        }

    }

    // ======================== END OF ONCREATE FUNCTION ====================================== //

    // ======================== START OF SOCKET LISTENERS====================================== //
    public void runGame() {
        // socket listener to be a voter
        socket.on("voter", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        Log.i("Game start", "start");
                        isVoter = true;
                        updateInterface(isVoter); // encapsulates the User interface
                    }
                });
            }
        });

        // socket listener to be an answerer
        socket.on("answerer", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        isVoter = false;
                        updateInterface(isVoter); // encapsulates the User interface
                    }
                });
            }
        });

        // socket listener to display the question card
        socket.on("question", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final Object[] myArgs = args;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_question.setText(myArgs[0].toString()); // changes the question textView to the question card dealt to everyone
                    }
                });
            }
        });

        // socket listener to display the answer cards in hand
        socket.on("answer", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONArray jArray = (JSONArray) args[0];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hand.clear(); // clear the hand first
                        // then update the hand with the updated hand in the server
                        for (int item = 0; item < jArray.length(); item++) {
                            try {
                                String tempString = (String) jArray.get(item);
                                hand.add(tempString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        recyclerviewAdaptorHand.notifyDataSetChanged();
                    }
                });
            }
        });

        // socket listener to display the time left in the round
        socket.on("timeLeftInRound", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_timer.setTextColor(Color.parseColor("#FA8072")); // color is red to signify the time left to pick an answer from hand
                        int timeLeftSeconds = Integer.parseInt(args[0].toString());
                        int minutes = timeLeftSeconds / 60;
                        int seconds = timeLeftSeconds % 60;
                        textView_timer.setText(String.format("%02d:%02d", minutes, seconds));
                    }
                });
            }
        });

        // socket listener to display the time left for voter to vote
        socket.on("timeLeftToVote", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_timer.setTextColor(Color.parseColor("#00A86B")); // color is green to signify the time left to vote for the winning card
                        int timeLeftSeconds = Integer.parseInt(args[0].toString());
                        int minutes = timeLeftSeconds / 60;
                        int seconds = timeLeftSeconds % 60;
                        textView_timer.setText(String.format("%02d:%02d", minutes, seconds));
                    }
                });
            }
        });

        // socket listener to display the time left for rest
        socket.on("timeLeftToRest", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_timer.setTextColor(Color.parseColor("#FFFF00")); // color is yellow to signify the time left to  for resting
                        int timeLeftSeconds = Integer.parseInt(args[0].toString());
                        int minutes = timeLeftSeconds / 60;
                        int seconds = timeLeftSeconds % 60;
                        textView_timer.setText(String.format("%02d:%02d", minutes, seconds));
                    }
                });
            }
        });

        // socket listener to update the play area
        socket.on("updatePlayArea", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONArray jArray = (JSONArray) args[0];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playArea.clear(); // same method as the updating of hand cards
                        for (int card = 0; card < jArray.length(); card++) {
                            try {
                                String tempString = (String) jArray.get(card);
                                playArea.add(tempString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Collections.sort(playArea); // Shuffle the cards just by sorting.
                        recyclerviewAdapterPlayArea.notifyDataSetChanged();
                    }
                });
            }
        });

        // socket listener to update the player scores
        socket.on("updatePlayerInfo", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONArray jArray = (JSONArray) args[0];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playerList.clear();
                        for (int player = 0; player < jArray.length(); player++) {
                            try {
                                JSONObject currPlayer = (JSONObject) jArray.get(player);
                                String currPlayerName = currPlayer.getString("name");
                                int currPlayerScore = currPlayer.getInt("score");
                                scoreMap.put(currPlayerName, currPlayerScore);
                                playerList.add(currPlayerName + " : " + currPlayerScore + "pts");// update the playerInfo listview
                                if (currPlayerName.equals(JoinGame.userName)) {//update score of current player
                                    score = currPlayerScore;
                                    textView_score.setText(Integer.toString(score));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Collections.sort(playArea); // Shuffle the cards just by sorting.
                    }
                });
            }
        });

        // socket listener to update round number
        socket.on("updateRoundNumber", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        round = (int) args[0];
                        textView_round.setText(Integer.toString(round));
                    }
                });
            }
        });

        // socket listener to turn the cards over when the time to answer is up
        socket.on("noMoreAnswering", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        canVote = true; // voter allowed to vote a card
                        if (isVoter) {
                            textView_annoucement.setText("Please cast your vote");
                        } else {
                            textView_annoucement.setText("Waiting for Captain to make the decision");
                        }
                        canAnswer = false; // players not allowed to answer or choose a card from the hand anymore
                    }
                });
            }
        });

        socket.on("winningPlayer", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONArray jArray = (JSONArray) args[0];
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        try {
                            // just the easiest way for me to display the winner.
                            // jArray.get(0) <-- gets the player's socketID (because server pass in socketID. Server should pass in the name in the future
                            // jArray.get(1) <-- gets the winning card chosen by the voter
                            String winnerName = jArray.get(0).toString();
                            String winningAns = jArray.get(1).toString();
                            textView_winnerName.setText(winnerName);
                            textView_winningAns.setText(winningAns);
                            cardview_winning.setVisibility(View.VISIBLE);
                            if (winnerName.equals(JoinGame.userName)) {
                                textView_annoucement.setText("You won! Points +1!\nThe next round is starting soon.");
                            } else {
                                textView_annoucement.setText(jArray.get(0).toString() + " won!\nThe next round is starting soon.");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // socket listener to trigger the event when there's only one player left in the room
        socket.on("onePlayerLeft", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("ALERT:Only one player left")
                                .setMessage("Please rejoin the game with another player(s) to play the game")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onBackPressed();
                                    }
                                })
                                .show();
                    }
                });
            }
        });

        // socket listener to end the game
        socket.on("gameEnd", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //socket.disconnect(); // player leaves the game after a game ends
                        startActivity(new Intent(getApplicationContext(), gameEnded.class));
                    }
                });
            }
        });

        // socket listener to display message from generalBroadcast
        socket.on("generalBroadcast", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String message = (String) args[0];
                            Log.i("Message:   ", message);
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // socket listener to disconnect from server
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_annoucement.setText("DISCONNECTED");
                    }
                });
            }
        });
    }

    // ======================== END OF SOCKET LISTENERS====================================== //

    // ======================== START OF HELPER FUNCTIONS ====================================== //
    public void updateInterface(boolean isVoter) {
        layout_submit.removeAllViews();
        cardview_winning.setVisibility(View.INVISIBLE);
        if (isVoter) {
            imageView_captain.setVisibility(View.VISIBLE);
            imageView_crew.setVisibility(View.INVISIBLE);
            recyclerViewPlayArea.setVisibility(View.VISIBLE);
            recyclerViewHand.setVisibility(View.INVISIBLE);
            textView_annoucement.setText("Please wait for all crews to submit their cards.");
            imageView_hands.setVisibility(View.INVISIBLE);
            imageView_battlefield.setVisibility(View.VISIBLE);
        } else {
            canAnswer = true;
            imageView_captain.setVisibility(View.INVISIBLE);
            imageView_crew.setVisibility(View.VISIBLE);
            recyclerViewPlayArea.setVisibility(View.INVISIBLE);
            recyclerViewHand.setVisibility(View.VISIBLE);
            textView_annoucement.setText("Drag and drop your chosen card!");
            imageView_hands.setVisibility(View.VISIBLE);
            imageView_battlefield.setVisibility(View.INVISIBLE);
        }
    }

    // setting up and calling the pop-up windown displaying player names and scores when button is pressed
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initPlayerListPop(View view) {
        ListPopupWindow playerListPop = new ListPopupWindow(getApplicationContext());
        playerListPop.setAdapter(new ArrayAdapter<>(getApplication(), R.layout.playerlist_popup_layout, playerList));
        playerListPop.setAnchorView(view);
        Drawable drawable = getResources().getDrawable(R.drawable.playerlist_background, getApplicationContext().getTheme());
        playerListPop.setBackgroundDrawable(drawable);
        playerListPop.setWidth(500);
        playerListPop.setHeight(600);
        playerListPop.show();
    }

    // Overriding the back button to disconnect when it is clicked
    @Override
    public void onBackPressed() {
        socket.disconnect(); // emits disconnection when player clicks back button

        // jump straight to the join game screen
        Intent intent = new Intent(getApplicationContext(), JoinGame.class);
        startActivity(intent);

        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(hideSystemBars());
        }
    }

    int hideSystemBars() {
        return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }

    // ======================== END OF HELPER FUNCTIONS ====================================== //
}
