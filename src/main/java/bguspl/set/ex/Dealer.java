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
        for(Player player : players)
        {
            Thread pl = new Thread(player);
            pl.start();
        }
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
        synchronized(table){
            this.terminate=true;
            for(Player player : players)
            {
                player.terminate();
            }
            env.ui.dispose();
        }
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

        // if there is no set on the table
        LinkedList<Integer> cardsOnTable = new LinkedList<>();
        for(int i = 0; i < table.slotToCard.length; i++)
        {
            cardsOnTable.add(table.slotToCard[i]);
        }
        if (!(env.util.findSets(cardsOnTable, 1).size() > 0) || System.currentTimeMillis() >= reshuffleTime)
        {
            removeAllCardsFromTable();
        }
        //if there is no set in the deck
        if (!(env.util.findSets(deck, 1).size()>0))
        {
            terminate();
        }
        // if there is no set then remove all cards from the table 
        checkSet();
    }
    //method we added:
    //cheack if it is a set and removing if nessserry
    private void checkSet() 
    {
  
        synchronized(table.playersWith3Tokens){
            Iterator<Integer> iter=table.getPlayersWith3Tokens().iterator();
            while(iter.hasNext())
            {
                int playerwith3=(int)iter.next();
                int playerId=0;
                for(Player p: players)
                {
                    if(p.id==playerwith3)
                        break;
                    else
                    playerId++;
                }
                //BlockingQueue<Integer> actions= players[player].getActions();
                int [] cards=new int[3];
                int i = 0;
                for(int slot = 0; slot < table.slotsOfPlayers[playerId].length; slot++)
                {
                    if(table.slotsOfPlayers[playerId][slot])
                    {
                        cards[i] = table.slotToCard[slot];
                        i++;
                    }
                }
                /*int j = 0;
                for(int item : actions)
                {
                    cards[j]= table.slotToCard[item];
                    j++;
                }*/
                if(env.util.testSet(cards)) 
                {
                    players[playerId].point();
                    try {
                        Thread.sleep(env.config.pointFreezeMillis);
                    } catch (InterruptedException ignored) {}
                    updateTimerDisplay(true);
                    // this happens in the player when he gets a point
                    /*for(int m = 0; m<cards.length; m++)
                    {
                        table.removeCard(cards[m]);
                        table.removeToken(playerwith3,cards[m]);
                        table.updatePlayersWith3Tokens(playerwith3);
                    }*/
                }
                else{
                    players[playerId].penalty();
                    try {
                        Thread.sleep(env.config.penaltyFreezeMillis);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        synchronized(table){
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
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        
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
