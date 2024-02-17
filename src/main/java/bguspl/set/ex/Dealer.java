package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        reshuffleTime = env.config.turnTimeoutMillis;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            updateTimerDisplay(false);
            placeCardsOnTable();
            timerLoop();
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            removeCardsFromTable();
            updateTimerDisplay(false);
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        this.terminate=true;
        //??? maybe
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards that should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement

        // if there is no set then remove all cards from the table 


    }
    //method we added:
    //cheack if it is a set and removing if nessserry
    private void checkSet() 
    {
        int i=0;
        Iterator<Integer> iter=table.getPlayersWith3Tokens().iterator();
        while(iter.hasNext())
        {
            int playerwith3=(int)iter.next();
            for(Player p: players)
            {
                if(p.id==playerwith3)
                    break;
                else
                    i++;
            }
            LinkedList<Integer> actions= players[i].getActions();
            int [] cards=new int[3];
            for(int j=0; j< cards.length;j++)
            {
                cards[j]=actions.get(j);
            }
            ///>>????
            if(env.util.testSet(cards))
            {
                for(int i = 0; i<cards.length; i++)
                {
                    table.removeCard(cards[i]);
                }
            }
        }

        // check if there is a set on the table
        
        
       

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement

        Collections.shuffle(deck);

        // check if there are any empty slots
        if(table.countCards()<table.slotToCard.length) // check if the table isn't full
        {
            for(int i = 0; i<table.slotToCard.length; i++)
            {
                if(table.slotToCard[i] == null && !deck.isEmpty())
                {
                    table.placeCard( deck.remove(0), i); // fill empty slot
                }
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement

        
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement

        if(reset || reshuffleTime - System.currentTimeMillis()<=0)
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        
        
        env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis);
        
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        
        for(int slot =0; slot < table.slotToCard.length; slot++)
        {
            for(Player p : players)
            {
                table.removeToken(p.id, slot);
                p.clearPlayerTokens();
                table.updatePlayersWith3Tokens(p.id);
            }
            if(table.slotToCard[slot] != null)
                deck.add(table.slotToCard[slot]);
            table.removeCard(slot);
        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement

        int maxScore = -1;
        List<Integer> winners= new LinkedList<Integer>();

        // get max score
        for(Player p:players)
        {
            if (p.score()>maxScore)
            {
                maxScore= p.score();
            }
        }

        // create list of winners id's
        for(Player p:players)
        {
            if (p.score()==maxScore)
            {
                winners.add(p.id);
            }
        }

        // convert list to int[]
        int[] winnersArray= new int[winners.size()];

        for(int i = 0; i < winners.size(); i++)
        {
            winnersArray[i] = winners.get(i);
        }

        env.ui.announceWinner(winnersArray);
    
    }
}
