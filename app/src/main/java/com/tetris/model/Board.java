package com.tetris.model;


import android.os.SystemClock;

import com.tetris.model.impl.ShapeShort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;


public class Board{

    public static final int BOARD_COLS = 10;
    public static final int BOARD_ROWS = 20;

    private static Board instance = null;

    private List<Block> blocks = new CopyOnWriteArrayList<>();
    private Shape fallingShape;
    private Shape nextShape;
    private Shape fastShape;

    private int score = 0;

    private GameStatus gameStatus;

    public enum GameStatus {
        INITIATING,
        IN_PROGRESS,
        PAUSED,
        GAME_OVER,
    }

    private List<Actions> actions = new CopyOnWriteArrayList<>();

    public enum Actions {
        DEAD_BLOCK,
        RESET_DEAD,
        COLLISION,
    }

    protected long last_deadLine_update = SystemClock.uptimeMillis();
    protected long last_fast_shape_update = SystemClock.uptimeMillis();

    protected int spawnY = -4;

    private int squareGameOver=0;

    private int deadBlockY =-2;

    private static HashMap colorMap = new HashMap <Integer,Integer>();

    //Board instance for use by other classes
    public static Board getInstance() {
        if (instance == null) {
            instance = new Board();
            //Initialize color map
            for(int color = 0; color <= 7; color++)
                colorMap.put(color, color);
        }
        return instance;
    }


    //Construct next shape randomly
    public void spawnNextShape() {
        Random r = new Random();
        int index = r.nextInt(7);
        //Get the color assign to the shape
        int color = (int) colorMap.get(index);

        nextShape = Shape.randomShape(index, spawnY, color);
        actions.add(Actions.COLLISION);
    }

    //Next shape falls
    public void makeNextShapeFalling() {
        if (nextShape == null) { //If there is no falling shape, next one is new one
            spawnNextShape();
        }
        if (fallingShape == null) { //If there is no falling shape, next one is new one
            fallingShape = nextShape;
            spawnNextShape();
        }
    }

    public boolean checkDeleteLinesUpdate() {
        long deleteLines = 50000;//TODO: cambiar time de 100s a 50s
        if (SystemClock.uptimeMillis() - last_deadLine_update > deleteLines) {
            last_deadLine_update = SystemClock.uptimeMillis();
            deadBlockY = deadBlockY + 2;
            actions.add(Actions.DEAD_BLOCK);
            return true;
        }
        return false;
    }


    public boolean checkFastShapeUpdate() {
        long fastShape = 10000;//TODO: cambiar time de 20s a 30s
        if (SystemClock.uptimeMillis() - last_fast_shape_update > fastShape) {
            return true;
        }
        return false;
    }

    //Updates the falling shape
    public void update() {
        if (checkDeleteLinesUpdate()) {
            spawnY = spawnY + 2;
        }
        if (checkFastShapeUpdate()) {
            if (fastShape == null) {
                fastShape = new ShapeShort(spawnY, (int) colorMap.get(7));
            } else {//Update fast shape
                fastShape.update();
                if (!fastShape.isFalling()) { //if it has collided
                    //Add fast shape blocks to board
                    for (Block block : fastShape.getBlocks()) {
                        block.setFalling(false);
                        blocks.add(block);
                    }

                    Shape layingShape = fastShape;
                    deleteLinesOf(layingShape);

                    if (checkGameOver()) {
                        gameStatus = GameStatus.GAME_OVER;
                    }

                    fastShape = null;

                    actions.add(Actions.COLLISION);
                    last_fast_shape_update = SystemClock.uptimeMillis();
                }
            }
        }

        if (fallingShape == null) { //Checks if the falling shape collided
            makeNextShapeFalling();
        } else {
            fallingShape.update();

            //EasterEggs.easterEgg1();
            if (!fallingShape.isFalling()) { //If it has collided with something
                //Add falling shape blocks to board
                for (Block block : fallingShape.getBlocks()) {
                    block.setFalling(false);
                    blocks.add(block);
                }

                Shape layingShape = fallingShape;
                deleteLinesOf(layingShape);

                if (checkGameOver()) {
                    gameStatus = GameStatus.GAME_OVER;
                } else if (fastShape != null) {  //Give movement controls to the fast shape
                    fallingShape = fastShape;
                    fastShape = null;
                    last_fast_shape_update = SystemClock.uptimeMillis();
                } else {
                    fallingShape = null;
                    makeNextShapeFalling();
                }
                actions.add(Actions.COLLISION);
            }
        }
    }

    //Deletes the lines that the shape is touching
    void deleteLinesOf(Shape shape) {
        List<Integer> deletedLines = new ArrayList<>();
        int numberLinesComplete = 0;
        //For each line the shape touches checks if its completed
        for (Block shapeBlock : shape.getBlocks()) {
            if (lineComplete(shapeBlock.getY())) { //If line is complete
                score += 30;
                numberLinesComplete++;

                deletedLines.add(shapeBlock.getY());
                // Remove from blocks all the block belonging to the same line.
                for (Block block : blocks) {
                    if (block.getY() == shapeBlock.getY()) //Remove block from Board if its in the line
                        blocks.remove(block);
                }
            }
        }

        if(numberLinesComplete == 4){
            resetDeadBlocks();
            randomColor();
            assignColor();
        }else if(numberLinesComplete > 0){
            colorForAll();
            assignColor();
        }

        //chooseColor(shape.getBlocks()[0]);
        //removeColors();
        for (Block block : blocks) {
            int count = 0;
            for (int y : deletedLines) {
                //Checks if the block its above one of the deleted lines
                if (y >= block.getY())
                    ++count;
            }
            //Moves block down per deleted line
            block.setY(block.getY() + count);
        }
    }

    private void randomColor(){
        Random r = new Random();
        int index;

        for(int color = 0; color <= 7; color++){
            do {
                index = r.nextInt(8);
                //Repeat until index is different from original color
                // and the color is not the same as in the hash map
            }while (index!=color && (!colorMap.get(index).equals(color)));
            //Add random color to related color
            colorMap.put(color, index);
        }
    }

    private void colorForAll(){
        //Get original color of the last falling shape
        int shapeColor = fallingShape.getBlocks()[0].getColor();
        //Put original shapeColor to all
        for(int color = 0; color <= 7; color++){
            colorMap.put(color, shapeColor);
        }

    }

    private void assignColor(){
        //Assign color to board blocks
        for(Block block : blocks){
            int actualColor = block.getColorNow();
            block.setColorNow((int) colorMap.get(actualColor));
        }
        //Assign color to nextShape blocks
        for(Block block : nextShape.getBlocks()){
            int actualColor = block.getColorNow();
            block.setColorNow((int) colorMap.get(actualColor));
        }

        if(fastShape != null) { //Assign color to fastShape blocks if exists
            for (Block block : fastShape.getBlocks()) {
                int actualColor = block.getColorNow();
                block.setColorNow((int) colorMap.get(actualColor));
            }
        }
    }

    //Checks if a line is complete
    private boolean lineComplete(int y) {
        int count = 0;
        for (Block block : blocks) {
            if (block.getY() == y)
                ++count;
        }
        return count == BOARD_COLS;
    }

    public boolean checkMoveLeft() {
        if (fallingShape == null) //If there is no falling shape cant move
            return false;
        fallingShape.moveLeft(); //Move shape to check block after movement
        if (fallingShape.collide()) { //Check if shape collided
            fallingShape.moveRight(); //If collided move back
            return false;
        }
        fallingShape.moveRight(); //If not move back and tell that it can move
        return true;
    }

    public boolean checkMoveRight() {
        if (fallingShape == null) //If there is no falling shape cant move
            return false;
        fallingShape.moveRight(); //Move shape to check block after movement
        if (fallingShape.collide()) { //Check if shape collided
            fallingShape.moveLeft(); //If collided move back
            return false;
        }
        fallingShape.moveLeft(); //If not move back and tell that it can move
        return true;
    }

    public void checkMoveDown() {
        boolean aux = true;
        while (aux) {
            fallingShape.moveDown();
            if (fallingShape.collide()) { //Check if shape collided
                fallingShape.moveUp();
                aux = false;
            }
        }
    }

    public boolean checkRotate() {
        if (fallingShape == null) //If there is no falling shape cant rotate
            return false;
        fallingShape.rotate(); //Move shape to check block after movement
        if (fallingShape.collide()) { //Check if shape collided
            // try to move right
            fallingShape.moveRight();
            if (fallingShape.collide())
                fallingShape.moveLeft();
            else {
                fallingShape.moveLeft();
                fallingShape.unrotate();
                fallingShape.moveRight();
                return true;
            }

            // try to move left
            fallingShape.moveLeft();
            if (fallingShape.collide())
                fallingShape.moveRight();
            else {
                fallingShape.moveRight();
                fallingShape.unrotate();
                fallingShape.moveLeft();
                return true;
            }

            fallingShape.unrotate();
            return false;
        }
        fallingShape.unrotate(); //If not rotated undo and tell that it cant
        return true;
    }

    private boolean checkGameOver() {
        for (Block block : blocks)
            if (block.getY() <= squareGameOver) //If any of the blocks Y coordinate is above the board limit
                return true;
        return false;
    }

    public void resetDeadBlocks(){
        actions.add(Actions.RESET_DEAD);

        last_deadLine_update = SystemClock.uptimeMillis();
        spawnY = -4;
        squareGameOver = 0;
        deadBlockY = -2;
        fallingShape.setY(-4);
        nextShape.setY(-4);
    }
  
    public void clear() {
        blocks.clear();
        fallingShape = null;
        nextShape = null;
        fastShape = null;
        score = 0;
        spawnY = -4;
        setDeadBlockY(-2);
        setSquareGameOver(0);
        actions.clear();
        last_deadLine_update = SystemClock.uptimeMillis();
        last_fast_shape_update = SystemClock.uptimeMillis();
        gameStatus = GameStatus.INITIATING;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Actions> getActions() {
        return actions;
    }

    public Shape getFallingShape() {
        return fallingShape;
    }

    public Shape getNextShape() {

        return nextShape;
    }

    public Shape getFastShape() {
        return fastShape;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getDeadBlockY() {
        return deadBlockY;
    }

    public void setDeadBlockY(int deadBlockY) {
        this.deadBlockY = deadBlockY;
    }

    public int getSquareGameOver() {
        return squareGameOver;
    }

    public void setSquareGameOver(int squareGameOver) {
        this.squareGameOver = squareGameOver;
    }
}
