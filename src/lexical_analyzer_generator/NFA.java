package lexical_analyzer_generator ;

import java.util.HashSet;

public class NFA  {

    private NFA_State start_state;
    private HashSet<NFA_State> accepting_states;
    private HashSet<NFA_State> states;

    public NFA(NFA_State start_state) {
        this.start_state = start_state;
        accepting_states = new HashSet<>();
        states = new HashSet<>();
    }

    public NFA() {
        accepting_states = new HashSet<>();
        states = new HashSet<>();
    }
    public void add_accepting_state(NFA_State end){
        accepting_states.add(end);
    }
    public HashSet<NFA_State> getStates() {
        return states;
    }

    public NFA_State getStart_state() {
        return start_state;
    }

    public void setStart_state(NFA_State start_state) {
        this.start_state = start_state;
    }

    public HashSet<NFA_State> getAccepting_states() {
        return accepting_states;
    }

    public void setAccepting_states(HashSet<NFA_State> accepting_states) {
        this.accepting_states = accepting_states;
    }

    public boolean is_Accepting_State ( NFA_State s ){
        return accepting_states.contains(s);
    }

}