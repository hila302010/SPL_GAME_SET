package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//IMPORTS WE ADDED:
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    //field we added
    protected volatile BlockingQueue<Integer> playersWith3Tokens;
    protected boolean[][] slotsOfPlayers;
    
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {
        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        playersWith3Tokens=new LinkedBlockingQueue<Integer>();
        slotsOfPlayers= new boolean[env.config.computerPlayers+env.config.humanPlayers][slotToCard.length];
        for(int i=0; i<slotsOfPlayers.length;i++)
        {
            for(int j=0; j<slotsOfPlayers[i].length; j++)
            {
                slotsOfPlayers[i][j] = false;
            }
        }
        
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        // TODO implement
         synchronized(cardToSlot)
         {
            cardToSlot[card] = slot;
            slotToCard[slot] = card;
            env.ui.placeCard(card,slot);
         }
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public synchronized void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        // TODO implement
            if (slotToCard[slot] != null)
            {
                env.ui.removeCard(slot);
                int card=slotToCard[slot];
                slotToCard[slot]=null;
                cardToSlot[card]=null;
            }
            for(int i = 0; i < slotsOfPlayers.length; i++)
            {
                slotsOfPlayers[i][slot] = false;
            }
    }


    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        
            if (slotToCard[slot] != null)
            {
                env.ui.placeToken(player,slot);
            }
            slotsOfPlayers[player][slot] = true;
        
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        if(slotToCard[slot]==null)
            return false;
        slotsOfPlayers[player][slot] = false;
        env.ui.removeToken(player,slot);  
        return true;
    }



    //methods we added
    public int getNumberOfTokens(int id)
    {
        int tokens = 0;
        for(int slot = 0; slot < slotsOfPlayers[id].length; slot++)
        {
            if(slotsOfPlayers[id][slot] == true)
            {
                tokens++;
            }
        }
        return tokens;
    }
    
    public BlockingQueue<Integer> getPlayersWith3Tokens() {
        return playersWith3Tokens;
    }

    //removing a player to the blocking queue
    public void updatePlayersWith3Tokens(int id) { 
        if(playersWith3Tokens.contains(id))
            playersWith3Tokens.remove(id);
           
    }
    //adding a player to the blocking queue
    public void addPlayerWith3Tokens(int id) {
         if(!playersWith3Tokens.contains(id))
            playersWith3Tokens.add(id);
    }
}