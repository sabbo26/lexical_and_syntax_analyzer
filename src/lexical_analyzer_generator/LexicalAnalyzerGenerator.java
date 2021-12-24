package lexical_analyzer_generator ;

import java.io.*;

import java.util.*;

public class LexicalAnalyzerGenerator {
    HashMap<String,NFA> reg_exp = new HashMap<>();
    HashMap<String,NFA> reg_def = new HashMap<>();
    HashMap<String,Integer> priorities = new HashMap<>();
    HashMap<String,Integer> opPriorities = new HashMap<>();
    HashSet<Character> alphabet = new HashSet<>();
    HashMap<NFA_State,String> mapping_combined_nfa_accepting_states_to_tokens ;
    HashSet<String> symbolTable = new HashSet<>();

    NFA automata;
    DFA temp;

    /* Current State*/
    BufferedReader br ;
    BufferedWriter bw;

    String str = "" ;

    int indx = 0;

    boolean fromQueue =false , NotError =false ;

    Queue<Character> queue = new LinkedList<>();
    /*Current State */

    public LexicalAnalyzerGenerator(){
        File input = new File("input_RegEx.txt");
        parse_file(input);
        this.automata = combine_NFA();
        this.temp = convert_to_DFA(this.automata);
        this.temp = minimize_DFA(this.temp);
        try {
            br = new BufferedReader(new FileReader("test.txt"));
            bw = new BufferedWriter(new FileWriter("output_tokens.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parse_file(File file) {
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        opPriorities.put("(", 0);
        opPriorities.put("|", 1);
        opPriorities.put(" ", 2);
        opPriorities.put("*", 3);
        opPriorities.put("+", 3);
        int highestPrioritySoFar = 0;
        while (sc.hasNextLine()) {
            String str = sc.nextLine();
            str = removeSpaces(str);
            String[] words = str.split(" ");
            HashMap<String,NFA> tmp = new HashMap<>();
            splitt(words, tmp);
            if (words[0].charAt(words[0].length() - 1) == ':') {
                str = words[0].substring(0, words[0].length() - 1);
                reg_exp.put(str, regDefExpStack(words, tmp, false));
                priorities.put(str, ++highestPrioritySoFar);
            } else if (words[1].equals("=")) {
                reg_def.put(words[0], regDefExpStack(words, tmp, true));
            } else {
                if (words[0].charAt(0) == '{') {
                    Keypunct(words, true, highestPrioritySoFar);
                } else if (words[0].charAt(0) == '[') {
                    Keypunct(words, false, highestPrioritySoFar);
                }
            }
        }
    }

    private NFA regDefExpStack(String[] words, HashMap<String,NFA> tmp, boolean def) {
        Stack<NFA> nfas = new Stack<>();
        Stack<String> op = new Stack<>();
        int i = def ? 2 : 1;
        int st = i;
        for (; i < words.length; i++) {
            if ((tmp.get(words[i]) != null || words[i].equals("("))
                    && i - 1 >= st && !words[i - 1].equals("|") && !words[i - 1].equals("(")) {
                while (!op.isEmpty() && opPriorities.get(op.peek()) > opPriorities.get(" ")) {
                    compOp(nfas, op);
                }
                op.push(" ");
                if (!words[i].equals("(")) nfas.push(tmp.get(words[i]));
                else op.push("(");
            } else if (words[i].equals("|")) {
                while (!op.isEmpty() && opPriorities.get(op.peek()) > opPriorities.get("|")) {
                    compOp(nfas, op);
                }
                op.push("|");
            } else if (words[i].equals("*") || words[i].equals("+") || words[i].equals("(")) {
                op.push(words[i]);
            } else if (words[i].equals(")")) {
                while (!op.peek().equals("(")) {
                    compOp(nfas, op);
                }
                op.pop();
            } else if (tmp.get(words[i]) != null) nfas.push(tmp.get(words[i]));
        }
        while (!op.isEmpty()) {
            compOp(nfas, op);
        }
        return nfas.pop();
    }

    private void compOp(Stack<NFA> nfas, Stack<String> op) {
        switch (op.peek()) {
            case "*":
                nfas.push(kleene_closure(nfas.pop()));
                op.pop();
                break;
            case "+":
                nfas.push(positive_closure(nfas.pop()));
                op.pop();
                break;
            case " ": {
                NFA sec = nfas.pop();
                NFA fir = nfas.pop();
                nfas.push(and_NFA(fir, sec));
                op.pop();
                break;
            }
            case "|": {
                NFA sec = nfas.pop();
                NFA fir = nfas.pop();
                nfas.push(or_NFA(fir, sec));
                op.pop();
                break;
            }
        }
    }

    private void Keypunct(String[] words, boolean keyword, int highestPriority) {
        for (int i = 1; i < words.length - 1; i++) {
            NFA n = getNFA(words[i]);
            if (words[i].contains("\\")){
                StringBuilder tmp =  new StringBuilder();
                for(int j = 0; j < words[i].length(); j++){
                    if (words[i].charAt(j) != '\\') tmp.append(words[i].charAt(j));
                }
                words[i] = tmp.toString();
            }
            int put = keyword ? 0 : ++highestPriority;
            reg_exp.put(words[i], n);
            priorities.put(words[i], put);
        }
    }

    private void splitt(String[] words, HashMap<String,NFA> tmp) {
        String s;
        for (int i = 1; i < words.length; i++) {
            s = words[i];
            if (!s.equals("(") && !s.equals("|") && !s.equals(")")
                    && !s.equals("*") && !s.equals("+") && !s.equals("{")
                    && !s.equals("}") && !s.equals("[") && !s.equals("]") && !s.equals("=")) {
                tmp.put(s, getNFA(s));
            }
        }
    }

    private NFA getNFA(String str) {
        if (str.length() == 1) {
            alphabet.add(str.charAt(0));
            return convert_to_NFA(str.charAt(0));
        } else if (reg_def.get(str) != null)
            return reg_def.get(str);
        else if (str.contains("-"))
            return fromto(str.indexOf("-"), str);
        else {
            NFA nfa = null;
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '\\') {
                    if (str.charAt(i + 1) == 'L')
                        nfa = nfa == null ? convert_to_NFA(null) : and_NFA(nfa, convert_to_NFA(null));
                    else {
                        nfa = nfa == null ? convert_to_NFA(str.charAt(i + 1)) : and_NFA(nfa, convert_to_NFA(str.charAt(i + 1)));
                        alphabet.add(str.charAt(i + 1));
                    }
                    i++;
                } else {
                    nfa = nfa == null ? convert_to_NFA(str.charAt(i)) : and_NFA(nfa, convert_to_NFA(str.charAt(i)));
                    alphabet.add(str.charAt(i));
                }
            }
            return nfa;
        }
    }

    public NFA fromto(int i, String word) {
        char from, to;
        from = word.charAt(i - 1);
        to = word.charAt(i + 1);
        NFA nfa = convert_to_NFA(from);
        alphabet.add(from);
        for (char j = (char) (from + 1); j <= to; j++) {
            alphabet.add(j);
            nfa = or_NFA(nfa, convert_to_NFA(j));
        }
        return nfa;
    }

    public String removeSpaces(String s) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            if (s.charAt(i) == ' ') {
                if (i < s.length() - 2 && s.charAt(i + 1) == '-' && s.charAt(i + 2) == ' ') {
                    res.append('-');
                    i += 3;
                } else if (i != 0) {
                    res.append(' ');
                    i++;
                }
                while (i < s.length() && s.charAt(i) == ' ')
                    i++;
            } else {
                boolean check = i + 1 < s.length() && s.charAt(i + 1) != ' ';
                if ((i == 0 || i == s.length() - 1) && (s.charAt(i) == '{' || s.charAt(i) == '['
                        || s.charAt(i) == '}' || s.charAt(i) == ']')) {
                    if (i - 1 >= 0)
                        res.append(' ');
                    res.append(s.charAt(i));
                    if (check) res.append(' ');
                } else if ((s.charAt(i) == '(' || s.charAt(i) == ')' || s.charAt(i) == '*'
                        || s.charAt(i) == '|' || s.charAt(i) == '+') && (i - 1 >= 0 && s.charAt(i - 1) != '\\')) {
                    if (res.charAt(res.length() - 1) != ' ')
                        res.append(' ');
                    res.append(s.charAt(i));
                    if (check) res.append(' ');
                } else
                    res.append(s.charAt(i));
                i++;
            }
        }
        int len = res.length();
        if (res.charAt(len - 1) == ' ') res.delete(len - 1, len);
        return res.toString();
    }

    public NFA convert_to_NFA ( Character s ){

        NFA_State start=new NFA_State();
        NFA_State end=new NFA_State();

        if (s==null)
            start.getEmpty_transitions().add(end);
        else
            start.insert_transition(s,end);

        NFA res=new NFA();

        res.add_accepting_state(end);
        res.getStates().add(end);
        res.getStates().add(start);
        res.setStart_state(start);
        return res;
    }

    public DFA convert_to_DFA ( NFA automata  ){

        // mapping each nfa state to a set of nfa states represents its closure

        HashMap<NFA_State , HashSet<NFA_State> > closure = new HashMap<>();

        // calculate closure for every state in automata

        for ( NFA_State state : automata.getStates() ){
            HashSet<NFA_State> set = new HashSet<>();
            calc_closure( state , set );
            closure.put( state , set );
        }

        // mapping set of nfa_states to one dfa state

        HashMap< HashSet<NFA_State> , DFA_State > mapping_set_of_nfa_states_to_one_dfa_state = new HashMap<>();

        DFA_State start_state = new DFA_State();

        mapping_set_of_nfa_states_to_one_dfa_state.put(  closure.get( automata.getStart_state() ) , start_state  );

        construct_dfa_states( closure.get( automata.getStart_state() )  , mapping_set_of_nfa_states_to_one_dfa_state , closure );

        DFA sol = new DFA();

        // add states to our solution

        // adding states to dfa and adjust accepting and start state

        for (Map.Entry< HashSet<NFA_State> , DFA_State > x : mapping_set_of_nfa_states_to_one_dfa_state.entrySet() ){
            String token = null ;
            int priority = Integer.MAX_VALUE ;
            // check if the set contains an accepting state and if more than one choose the highest priority
            for ( NFA_State w : x.getKey() ){
                if ( automata.is_Accepting_State(w) ){
                    String temp_2 = mapping_combined_nfa_accepting_states_to_tokens.get(w);
                    int k = priorities.get(temp_2);
                    if ( k < priority ){
                        token = temp_2;
                        priority = k ;
                    }
                }
            }
            if ( token != null )
                sol.add_Accepting_state(x.getValue(),token);
            sol.add_state(x.getValue());
        }
        sol.setStart_state(start_state);
        return sol ;
    }

    public void construct_dfa_states ( HashSet<NFA_State> set  ,
                                       HashMap< HashSet<NFA_State> , DFA_State > mapping_set_of_nfa_states_to_one_dfa_state,
                                       HashMap<NFA_State , HashSet<NFA_State> > closure ){

        DFA_State corresponding = mapping_set_of_nfa_states_to_one_dfa_state.get(set);

        for (Character x : alphabet ){
            HashSet<NFA_State> temp_1 = new HashSet<>();

            // calculate set of states corresponding to input symbol x
            for ( NFA_State state : set ){
                HashSet<NFA_State> y = state.transition(x);
                if ( y != null ){
                    for ( NFA_State q : y )
                        temp_1.addAll( closure.get(q) );
                }
            }
            if ( temp_1.size() == 0 )
                continue;
            boolean found = false ;
            //check if the set of nfa already exists
            for ( HashSet<NFA_State> r : mapping_set_of_nfa_states_to_one_dfa_state.keySet() ){
                if ( compare_two_sets(r, temp_1) ){
                    corresponding.insert_transition(x, mapping_set_of_nfa_states_to_one_dfa_state.get(r) );
                    found =true ;
                    break;
                }
            }
            // if not found map it to a new dfa state and construct its transition
            if ( !found ){
                DFA_State new_one = new DFA_State();
                corresponding.insert_transition(x,new_one);
                mapping_set_of_nfa_states_to_one_dfa_state.put(temp_1,new_one);
                construct_dfa_states( temp_1  , mapping_set_of_nfa_states_to_one_dfa_state , closure );
            }
        }

    }

    // compare two sets if they have the same nfa states
    public boolean compare_two_sets ( HashSet<NFA_State> x , HashSet<NFA_State> y ){

        if ( x.size() != y.size() )
            return false ;

        for ( NFA_State q : x ){
            if ( !y.contains(q) )
                return false ;
        }
        return true ;
    }
    //calculate closure for a nfa state
    public void calc_closure ( NFA_State state , HashSet<NFA_State> set ){

        if ( !set.contains( state ) ){
            set.add(state);
            for ( NFA_State x : state.getEmpty_transitions() )
                calc_closure(x,set);
        }

    }

    public DFA minimize_DFA ( DFA automata ){

        // set of sets , each set contains DFA states

        HashSet<HashSet< DFA_State >> partition = new HashSet<>();

        // partitioning DFA states to accepting and non accepting as first step

        HashMap<String , HashSet<DFA_State> > accepting_sets_based_on_tokens = new HashMap<>();

        //HashSet<DFA_State> accepting = new HashSet<>();

        HashSet<DFA_State> non_accepting = new HashSet<>();

        // map to know which set in partition contains a DFA state

        HashMap<DFA_State , HashSet<DFA_State>  > map_state_to_set_in_partition = new HashMap<>();

        HashSet<DFA_State> null_transition = non_accepting ;

        // partitioning states to accepting and non accepting

        for (DFA_State temp : automata.getStates()) {
            String temp_1 = automata.getAccepting_states().get(temp);
            if ( temp_1 != null ){

                HashSet<DFA_State> temp_2 = accepting_sets_based_on_tokens.get(temp_1);

                if ( temp_2 == null ){
                    temp_2 = new HashSet<>();
                    accepting_sets_based_on_tokens.put(temp_1,temp_2);
                }
                temp_2.add(temp);
            }
            else{
                non_accepting.add(temp);
                map_state_to_set_in_partition.put(temp,non_accepting);
            }
        }


        partition.add(non_accepting);

        partition.addAll( accepting_sets_based_on_tokens.values());

        // to keep track if number of sets is the same at the end of partitioning to stop partitioning process

        int size_of_old_partition ;

        while ( true ){

            size_of_old_partition = partition.size() ;

            for ( Character w : alphabet ){

                // each set is poped and partitioned by input symbol w

                Stack< HashSet<DFA_State> > stk = new Stack<>();

                for ( HashSet<DFA_State> group : partition )
                    stk.push(group);

                while ( !stk.isEmpty() ){

                    HashSet<DFA_State> group = stk.pop() ;

                    // map to keep track of which states go to the same set under input symbol w

                    HashMap<  HashSet<DFA_State> , HashSet<DFA_State>  > mapping = new HashMap<>();

                    for ( DFA_State x : group ){

                        DFA_State y = x.get_transition(w);

                        HashSet<DFA_State> temp_1 ;

                        if ( y == null )
                            temp_1 = null_transition ;
                        else
                            temp_1 = map_state_to_set_in_partition.get(y);

                        HashSet<DFA_State> answer  = mapping.get(temp_1);

                        if ( answer == null ){
                            answer = new HashSet<>();
                            mapping.put(temp_1,answer);
                        }
                        answer.add(x);

                    }

                    // remove old set and add new sets that result from partitioning

                    partition.remove(group);

                    partition.addAll(mapping.values());

                    null_transition = new HashSet<>();
                }

            }
            // breaking condition is that no change happened after partitioning
            if ( partition.size() == size_of_old_partition )
                break;
                // update mapping states to set in the new partition
            else{
                map_state_to_set_in_partition.clear();

                for ( HashSet<DFA_State> r : partition ){
                    for ( DFA_State u : r ){
                        map_state_to_set_in_partition.put( u , r ) ;
                    }
                }

            }

        }
        // the new dfa to construct solution
        DFA sol = new DFA();
        // mapping each set in partition to a new DFA state and adding these states to solution dfa
        HashMap< HashSet<DFA_State> , DFA_State > mapping_set_to_new_DFA_state = new HashMap<>();
        for ( HashSet<DFA_State> x : partition ){
            DFA_State w = new DFA_State();
            mapping_set_to_new_DFA_state.put( x , w );
            sol.add_state(w);
            // check if the set contains start or accepting states to mark the new dfa state as start of accepting
            for ( DFA_State y : x ){
                if ( automata.getAccepting_states().containsKey(y) )
                    sol.getAccepting_states().put( w , automata.getAccepting_states().get(y)  );
                if ( automata.getStart_state() == y )
                    sol.setStart_state(w);
            }
        }
        // adding transitions to the new dfa states
        for ( HashSet<DFA_State> x : partition ){
            DFA_State temp_4 = mapping_set_to_new_DFA_state.get(x);
            for ( DFA_State y : x ){
                for ( Map.Entry < Character , DFA_State > w : y.getTransitions().entrySet()  ){
                    DFA_State temp_1 = w.getValue() ;
                    HashSet<DFA_State> temp_2 = map_state_to_set_in_partition.get(temp_1);
                    DFA_State temp_3 = mapping_set_to_new_DFA_state.get(temp_2);
                    temp_4.insert_transition(w.getKey(),temp_3);
                }
                break;
            }
        }
        return sol ;
    }

    public String getNextToken() throws IOException {

        String lastAcceptState="" , token ="";
        StringBuilder identifier = new StringBuilder();
        DFA_State currentState = temp.getStart_state();
        char currentChar =' ';
        boolean getToken = false;

        if(str.equals("") || !(indx < str.length() || !queue.isEmpty())){
            str = br.readLine();
            indx = 0;
        }

        if(str == null) {
            br.close();
            bw.close();
            return null;
        }

        while (indx < str.length() || !queue.isEmpty()) {

            //read from Queue if it is not empty
            if (!queue.isEmpty() && !NotError) {
                fromQueue = true;
                currentChar = queue.poll();
            } else if (indx < str.length()) {
                fromQueue = false;
                NotError = false;
                currentChar = str.charAt(indx);
            }

            //change currentState under input currentChar
            currentState = currentState.get_transition(currentChar);


            if (currentChar != ' ') {
                queue.add(currentChar);
            }

            //assign lastAccpetState after check that currentState is not deadState and it is accpetState
            if (currentState != null && temp.checkAccpetState(currentState)) {
                lastAcceptState = temp.getToken(currentState);
                queue.clear();
                identifier.append(currentChar);
            }
                /*if transition go to DeadState 'currentState == null' and lastAcceptState is exist
                    write token in the output file
                    reset currentState to startState
                    if this token is identifier put it in symbolTable
                    reset StringBuilder
                 */
            else if (currentState == null && !lastAcceptState.equals("")) {

                bw.write(lastAcceptState);
                bw.newLine();

                currentState = temp.getStart_state();

                if (lastAcceptState.equals("id")) {
                    identifier.delete(identifier.length() - queue.size(), identifier.length());
                    symbolTable.add(identifier.toString());
                }

                identifier.delete(0, identifier.length());

                token = lastAcceptState ;
                getToken = true;

                lastAcceptState = "";
            }
                /*Error handling
                  remove first character
                  reset currentState to startState
                  continue looking for tokens.
                 */
            else if (currentState == null || (!temp.checkAccpetState(currentState) && indx >= str.length())) {
                if (currentChar != ' ') {
                    bw.write("Error");
                    bw.newLine();
                }
                queue.poll();
                currentState = temp.getStart_state();
                identifier.delete(0, identifier.length());
            } else {
                NotError = true;
                identifier.append(currentChar);
            }

            if (!fromQueue)
                indx++;

            if(getToken){
                break;
            }
        }

        if(!lastAcceptState.equals( "" ) ) {
            bw.write(lastAcceptState);
            bw.newLine();

            if (lastAcceptState.equals("id")){
                symbolTable.add(identifier.toString());
            }
            token = lastAcceptState;
        }

        return token;
    }

    public NFA kleene_closure(NFA nfa) {
        /* new start --(^)-> old start done
         * old final --(^)-> old start
         * old final --(^)-> new final
         * new start --(^)-> new final
         * set nfa start to be new start
         * set in nfa accepting states new final
         * remove old final from accepting states
         * set new start and new final in states set
         *  */
        NFA res= deepClone(nfa);
        NFA_State new_start=new NFA_State();
        NFA_State new_final=new NFA_State();
        NFA_State old_start=  res.getStart_state();
        HashSet<NFA_State> accept= res.getAccepting_states();
        NFA_State old_final= accept.iterator().next();
        HashSet<NFA_State> states=res.getStates();

        new_start.getEmpty_transitions().add(old_start);
        old_final.getEmpty_transitions().add(old_start);
        old_final.getEmpty_transitions().add(new_final);
        new_start.getEmpty_transitions().add(new_final);

        res.setStart_state(new_start);
        accept.remove(old_final);
        accept.add(new_final);
        res.setAccepting_states(accept);
        states.add(new_start);
        states.add(new_final);

        return  res;
    }

    public NFA or_NFA (NFA nfa1, NFA nfa2){
        /*
         * new start --(^)->old start1
         * new start --(^)->old start2
         * old final1 --(^)->new final
         * old final2 --(^)->new final
         * create NFA result & set start -->new start
         * move states in nfa1 and nfa2 to result nfa
         * set accepting state --> new final
         * */
        NFA clone_nfa1= deepClone(nfa1);
        NFA clone_nfa2=deepClone(nfa2);

        NFA_State new_start=new NFA_State();
        NFA_State new_final=new NFA_State();

        NFA_State old_start1=clone_nfa1.getStart_state();
        NFA_State old_final1=clone_nfa1.getAccepting_states().iterator().next();
        NFA_State old_start2=clone_nfa2.getStart_state();
        NFA_State old_final2=clone_nfa2.getAccepting_states().iterator().next();

        new_start.getEmpty_transitions().add(old_start1);
        new_start.getEmpty_transitions().add(old_start2);
        old_final1.getEmpty_transitions().add(new_final);
        old_final2.getEmpty_transitions().add(new_final);

        NFA res_nfa=new NFA(new_start);
        HashSet<NFA_State> res_states=res_nfa.getStates();
        res_states.add(new_start);
        res_states.addAll(clone_nfa1.getStates());
        res_states.addAll(clone_nfa2.getStates());
        res_states.add(new_final);
        res_nfa.add_accepting_state(new_final);

        return res_nfa;
    }


    public NFA and_NFA (NFA nfa1, NFA nfa2){

        /*
         *get final of nfa1 (final1)and the start of nfa2(start2)
         * remove the accepting states from nfa1
         * merge the two nfas by make final1 same as start2
         * add states from nfa2 to nfa
         * */
        NFA clone_nfa1= deepClone(nfa1);
        NFA clone_nfa2=deepClone(nfa2);

        NFA_State start2=clone_nfa2.getStart_state();
        NFA_State final1=clone_nfa1.getAccepting_states().iterator().next();

        clone_nfa1.getAccepting_states().remove(final1);
        final1.setTransitions(start2.getTransitions());
        final1.setEmpty_transitions(start2.getEmpty_transitions());
        clone_nfa2.getStates().remove(clone_nfa2.getStart_state());
        clone_nfa1.getStates().addAll(clone_nfa2.getStates());
        clone_nfa1.getAccepting_states().addAll(clone_nfa2.getAccepting_states());

        return clone_nfa1;
    }


    public NFA positive_closure(NFA nfa){
        NFA cloned=deepClone(nfa);
        return and_NFA(cloned,kleene_closure(cloned));
    }

    public NFA combine_NFA(){
        NFA_State start =new NFA_State();
        NFA combined=new NFA(start);
        mapping_combined_nfa_accepting_states_to_tokens=new HashMap<>();
        for (String st:reg_exp.keySet()){
            NFA nfa=reg_exp.get(st);
            NFA_State nfa_start=nfa.getStart_state();
            start.getEmpty_transitions().add(nfa_start);
            combined.add_accepting_state(nfa.getAccepting_states().iterator().next());
            combined.getStates().addAll(nfa.getStates());
            NFA_State n=nfa.getAccepting_states().iterator().next();
            mapping_combined_nfa_accepting_states_to_tokens.put(n,st);
        }
        combined.getStates().add(start);
        return combined;
    }

    private NFA deepClone(NFA nfa)
    {
        HashMap<NFA_State,NFA_State>created=new HashMap<>();
        NFA cloned=new NFA(dfs(nfa.getStart_state(),created));

        cloned.getStates().addAll(created.values());
        for (NFA_State nfa_state:nfa.getAccepting_states())
        {
            cloned.add_accepting_state(created.get(nfa_state));
        }
        return cloned;
    }

    private NFA_State dfs(NFA_State start_state, HashMap<NFA_State,NFA_State> created)
    {
        if (start_state==null)return null;


        if(created.containsKey(start_state)) {
            return created.get(start_state);
        }
        NFA_State currentNfa = new NFA_State();
        created.put(start_state, currentNfa);
        for(Character c:start_state.getTransitions().keySet()) {
            for (NFA_State n:start_state.getTransitions().get(c)){
                currentNfa.insert_transition(c,dfs(n,created));}
        }
        for (NFA_State n:start_state.getEmpty_transitions()){
            currentNfa.getEmpty_transitions().add(dfs(n,created));}
        return currentNfa;
    }
}