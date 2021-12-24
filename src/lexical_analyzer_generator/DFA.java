package lexical_analyzer_generator ;

import java.util.HashMap;
import java.util.HashSet;

public class DFA {

    private DFA_State start_state ;

    private HashMap< DFA_State , String > accepting_states ;

    private HashSet<DFA_State> states ;

    public DFA() {
        accepting_states = new HashMap<>();
        states = new HashSet<>();
    }

    public DFA_State getStart_state() {
        return start_state;
    }

    public void setStart_state(DFA_State start_state) {
        this.start_state = start_state;
    }

    public boolean checkAccpetState(DFA_State state){
        return accepting_states.get(state) != null;
    }

    public  String getToken(DFA_State state){
        return accepting_states.get(state);
    }

    public void add_Accepting_state (DFA_State s , String token ){
        accepting_states.put(s,token);
    }

    public void add_state (DFA_State state ){
        states.add(state);
    }

    public HashMap<DFA_State, String> getAccepting_states() {
        return accepting_states;
    }

    public HashSet<DFA_State> getStates() {
        return states;
    }
}
