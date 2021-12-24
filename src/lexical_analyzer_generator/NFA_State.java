package lexical_analyzer_generator ;


import java.util.HashMap;
import java.util.HashSet;

public class NFA_State{

    private HashMap<Character, HashSet<NFA_State>  > transitions ;
    private HashSet< NFA_State > empty_transitions ;

    public void setEmpty_transitions(HashSet<NFA_State> empty_transitions) {
        this.empty_transitions = empty_transitions;
    }

    public HashSet<NFA_State> getEmpty_transitions() {
        return empty_transitions;
    }

    public NFA_State() {
        transitions = new HashMap<>();
        empty_transitions = new HashSet<>();
    }

    public HashSet<NFA_State> transition (char input ){
        return transitions.get(input);
    }

    public void setTransitions(HashMap<Character, HashSet<NFA_State>> transitions) {
        this.transitions = transitions;
    }

    public HashMap<Character, HashSet<NFA_State>> getTransitions() {
        return transitions;
    }

    public void insert_transition ( char x , NFA_State s ){

        HashSet<NFA_State> set = transitions.get(x);

        if ( set == null ){
            set = new HashSet<>();
            transitions.put(x,set);
        }

        set.add(s);

    }


}