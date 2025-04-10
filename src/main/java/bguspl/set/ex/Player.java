package bguspl.set.ex;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.text.html.HTMLDocument.Iterator;

import bguspl.set.Env;
//imports we added
import java.util.LinkedList;
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
    private boolean freezeForPoint=false;
    private boolean freezeForPenalty=false;
    //private  long freezeTime;
    //private  long freezeCurrentTime;
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
        actions=new LinkedBlockingQueue<Integer>(env.config.featureSize);
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
          synchronized(actions) {
            try{
                actions.wait();
            }catch(InterruptedException e){}

            for(int slot : actions)
            {
                if(!table.slotsOfPlayers[id][slot])
                {
                    table.placeToken(id, slot);
                }
                else{
                    table.removeToken(id, slot);
                }
                actions.remove();
                /*if(table.slotToCard[slot]!=null) // there is a card in this slot
                {
                    if((!actions.contains(slot)) && (actions.size()<3))
                    {
                        // if the player didn't pick this slot yet
                        actions.add(slot);
                        table.placeToken(id,slot);
                        if(actions.size()==3){
                            table.addPlayerWith3Tokens(id);
                        }
                    }
                    else 
                    if(actions.contains(slot))
                    {
                        // if the player picked this slot before
                        // linkded list removes by index
                        for(int item : actions)
                        {
                            if(item == slot)
                            {
                                actions.remove(item);
                            }
                        }
                        table.removeToken(id,slot);
                        table.updatePlayersWith3Tokens(id);
                    }  
                }*/
            }

          }

          synchronized(this){
            if(table.getNumberOfTokens(id) == env.config.featureSize){
                table.addPlayerWith3Tokens(id);
                try{
                    this.wait();
                }catch(InterruptedException e){}
            }
            
          }


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
                try{
                    Thread.sleep(1000);
                }catch (InterruptedException ignored) {}
                // try {
                //     synchronized (this) { 
                //         wait(); 
                //     }
                // } catch (InterruptedException ignored) {}
                Random rnd = new Random();
                int rand = rnd.nextInt(12);
                keyPressed(rand);
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
        playerThread.interrupt();
        // interupt
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

        if(!freezeForPenalty && !freezeForPoint)
        {
            synchronized(actions)
            {
                if((!actions.contains(slot)) && (actions.size()<3))
                {
                  // if the player didn't pick this slot yet
                     actions.add(slot);
                     actions.notifyAll();
                }
            }
        }

        // if(table.slotToCard[slot]!=null) // there is a card in this slot
        // {
        //     if((!actions.contains(slot)) && (actions.size()<3))
        //     {
        //         // if the player didn't pick this slot yet
        //         actions.add(slot);
        //         table.placeToken(id,slot);
        //         if(actions.size()==3){
        //             table.addPlayerWith3Tokens(id);
        //         }
        //     }
        //     else 
        //     if(actions.contains(slot))
        //     {
        //         // if the player picked this slot before
        //         // linkded list removes by index
        //         for(int item : actions)
        //         {
        //             if(item == slot)
        //             {
        //                 actions.remove(item);
        //             }
        //         }
        //         table.removeToken(id,slot);
        //         table.updatePlayersWith3Tokens(id);
        //     }  
        // }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {

        // try {
        //     Thread.sleep(env.config.pointFreezeMillis);
        // } catch (InterruptedException ignored) {}

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

           // TODO implement
           freezeForPoint=true;
           updateTimerDisplay(env.config.pointFreezeMillis);
           //remove all tokens of this player from the table
           for(int slot : actions)
           {
                table.removeToken(id, slot);
                table.removeCard(slot);
           }  
           table.updatePlayersWith3Tokens(id);
           actions.clear();//blocking queue should be empty
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        freezeForPenalty=true;
        updateTimerDisplay(env.config.penaltyFreezeMillis);
        

        //freezing the thread 
        // try {
        //     Thread.sleep(env.config.penaltyFreezeMillis);
        // } catch (InterruptedException ignored) {}

        
    }

    public int score() {
        return score;
    }



    public void clearPlayerTokens() {

        actions.clear();//blocking queue should be empty
   
    }

    //methods we added
    public BlockingQueue<Integer> getActions()
    {
        return actions;
    }

    public void updateTimerDisplay(long time) {
       
        env.ui.setFreeze(id, time); 
        while(time>0)
        {
            try{
                Thread.sleep(env.config.pointFreezeMillis);
            } catch (InterruptedException ignored) {}
            time-=env.config.pointFreezeMillis;
            env.ui.setFreeze(id, time);  
        }

    }

}
