package lexical_analyzer_generator ;


import java.util.HashMap;

public class DFA_State {

    private HashMap<Character,DFA_State> transitions ;

    public HashMap<Character, DFA_State> getTransitions() {
        return transitions;
    }

    public DFA_State() {
        transitions = new HashMap<>();
    }

    public DFA_State get_transition ( char x ){
        return transitions.get(x);
    }

    public void insert_transition ( char x , DFA_State s  ){
        transitions.put(x,s);
    }

}