package com.tetris.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.tetris.R;
import com.tetris.model.Board;
import com.tetris.model.events.MovementEvents;
import com.tetris.view.layout_painting.BlockedBlocksLayout;
import com.tetris.view.layout_painting.BoardLayout;
import com.tetris.view.layout_painting.FallingShapeLayout;
import com.tetris.view.layout_painting.NextShapeLayout;

public class GameActivity extends Activity {

    boolean stopped = false;

    public static int BOARD_HEIGHT = 800; //Max quality = 6400 -> Laser-mode = 20
    public static int BOARD_WIDTH = 400; //Max quality = 3200 -> Laser-mode = 10
    public static int PIXEL_SIZE = BOARD_WIDTH / Board.BOARD_COLS;
    final Handler handler = new Handler();

    //Buttons
    public ImageButton despDer;
    public ImageButton despIzq;
    public ImageButton despRotate;
    public Button despDown;

    //Board values
    int speed = 50;

    Paint paint;

    public static ImageView boardLayout;
    public static ImageView fallingShapeLayout;
    ConstraintLayout scoreLayout;
    public static ImageView nextShapeLayout;
    public static ImageView deadBlocksLayout;

    TextView scoreText;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!Board.getInstance().getGameStatus().equals(Board.GameStatus.GAME_OVER)) {
                Board.getInstance().update(); //Updates the board
            }
            if (!Board.getInstance().getGameStatus().equals(Board.GameStatus.GAME_OVER)) {
                paintGame(); //Paints game board
                handler.removeCallbacks(this);
                handler.postDelayed(this, speed);
            } else {
                onStop();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        setUpLayouts();

        setUpButtons();

        gameInit();
    }

    private void setUpLayouts() {
        // Game board
        boardLayout = findViewById(R.id.game_board);
        BoardLayout.boardLayoutInit();


        //Falling shape
        fallingShapeLayout = findViewById(R.id.falling_shape);
        FallingShapeLayout.fallingShapeLayoutInit();

        //Score
        scoreLayout = findViewById(R.id.top_board);
        scoreText = (TextView) findViewById(R.id.score_text_view);

        //Next shape
        nextShapeLayout = findViewById(R.id.next_shape);
        NextShapeLayout.nextShapeLayoutInit();


        //DeadBlocks
        deadBlocksLayout = findViewById(R.id.dead_blocks);
        BlockedBlocksLayout.blockedBlocksLayoutInit();

    }

    private void setUpButtons() {
        //MoveRight button
        despDer = findViewById(R.id.mvRight);
        despDer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                MovementEvents.checkAndMoveRight();
            }
        });
        //MoveLeft button
        despIzq = findViewById(R.id.mvLeft);
        despIzq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                MovementEvents.checkAndMoveLeft();
            }
        });
        //MoveRotate button
        despRotate = findViewById(R.id.mvRotate);
        despRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                MovementEvents.checkAndRotate();
            }
        });
        //MoveDown button
        despDown = findViewById(R.id.mvDown);
        despDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MovementEvents.checkAndMoveDown();
            }
        });
    }

    private void gameInit() {
        // TODO: do more stuff like set score to 0 or prepare controls
        if (Board.getInstance().getBlocks().size() > 0)
            Board.getInstance().clear();

        scoreText.setText(String.valueOf(Board.getInstance().getScore()));

        stopped = false;
        Board.getInstance().setGameStatus(Board.GameStatus.INITIATING);
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, speed);
    }

    //Painting methods
    private void paintGame() {
        for (Board.Actions a : Board.getInstance().getActionList()) {
            if (a.equals(Board.Actions.COLLISION)) {
                //Update board layout
                BoardLayout.paintBlockArray(this.getResources());
                //Update score
                scoreText.setText(String.valueOf(Board.getInstance().getScore()));
            }
            if (a.equals(Board.Actions.DEAD_BLOCK)) {
                BlockedBlocksLayout.paintBlockedBlocks(this.getResources());
                Board.getInstance().setSquareGameOver(Board.getInstance().getSquareGameOver() + 2);
            }
            if (a.equals(Board.Actions.RESET_DEAD)) {
                BlockedBlocksLayout.deleteDeadBlocks();
            }
            Board.getInstance().getActionList().clear(); //Clear actions list
        }
        //Paint fallingShape Layout
        FallingShapeLayout.paintFallingShape(this.getResources());
        //Paint next shape on left side
        NextShapeLayout.paintNextShape(this.getResources());
    }


    @Override
    protected void onPause() {
        super.onPause();
        Board.getInstance().setGameStatus(Board.GameStatus.PAUSED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Board.getInstance().setGameStatus(Board.GameStatus.IN_PROGRESS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Board.getInstance().setGameStatus(Board.GameStatus.PAUSED);

        //handler.removeCallbacks(runnable);

        if (!stopped) {
            stopped = true;
            Intent intent = new Intent(this, FinalScoreActivity.class);
            startActivity(intent);
        }
    }


    public static void setBoardHeight(int boardHeight) {
        BOARD_HEIGHT = boardHeight;
    }

    public static void setBoardWidth(int boardWidth) {
        BOARD_WIDTH = boardWidth;
    }

    public static void setPixelSize(int pixelWidth) {
        PIXEL_SIZE = pixelWidth / Board.BOARD_COLS;
    }
}
