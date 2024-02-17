package bguspl.set.ex;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.text.html.HTMLDocument.Iterator;

import bguspl.set.Env;


/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;


    //FIELDS WE ADDED:
    private BlockingQueue<Integer> actions;//the slots chosen by the player, max size=3;
    //private final BlockingQueue<Integer> cardsTodo;
    //private final BlockingQueue[] Todo;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        actions=new LinkedBlockingQueue<>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) 
            createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
        }
        if (!human)
         try { 
            aiThread.join(); 
         } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

        //should check whther the thread is freezed!!!!!!!!!!!!

        if(table.slotToCard[slot]!=null && actions.size()<3)
        {
            if(!actions.contains(slot))
            {
                actions.add(slot);
                table.placeToken(id,slot);
            }
            // else  
            // {
            //     actions.remove(slot);
            //     table.removeToken(id,slot);
            // }  
        }
        else  
        {
            actions.remove(slot);
            table.removeToken(id,slot);
        } 
        if(actions.size()==3){
            table.addPlayerWith3Tokens(id);
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {

        try {
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException ignored) {}

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

           // TODO implement
           score++;

           //remove all token of this player from the table
           for(int i = 0; i<actions.size(); i++)
           {
                table.removeToken(id, (int) actions.toArray()[i]);
           }    
           
           actions.clear();//blocking queue should be empty
           //freezing the thread

         
   
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement

        //freezing the thread 
        try {
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException ignored) {}
        
    }

    public int score() {
        return score;
    }



    public void clearPlayerTokens() {

        actions.clear();//blocking queue should be empty
   
    }
}
