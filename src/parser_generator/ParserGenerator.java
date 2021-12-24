package parser_generator ;


import java.io.*;
import java.util.*;
import lexical_analyzer_generator.LexicalAnalyzerGenerator ;

public class ParserGenerator {

    final private ArrayList<Component> dummy_for_sync;
    final private ArrayList<Component> dummy_for_epsilon;
    final private HashMap<String, Terminal> terminal_map;
    final private HashMap<String, NonTerminal> non_terminal_map;
    private ArrayList<Component>[][] parsing_table;
    final private HashMap<NonTerminal, Integer> rows_map;
    final private HashMap<Terminal, Integer> columns_map;
    private NonTerminal start_nonTerminal ;

    public ParserGenerator() {
        dummy_for_epsilon = new ArrayList<>();
        dummy_for_sync = new ArrayList<>();
        terminal_map = new HashMap<>();
        non_terminal_map = new HashMap<>();
        rows_map = new HashMap<>();
        columns_map = new HashMap<>();
        start_work();
    }

    private void start_work (){
        File cfg = new File("input_grammar.txt");
        parse_grammar(cfg);
        eliminate_left_recursion();
        left_factor_for_all();
        preFirstFollow();
        compute_first_set_for_all();
        compute_follow_set_for_all();
        if (!computeParseTable())
            System.out.println("not LL(1) grammar");
        else {
            try {
                process_input();
            }catch (Exception e ){
                System.out.println("error while processing input");
            }
        }
    }

    public static void main(String[] args) {
        new ParserGenerator();
    }
    private void eliminate_left_recursion (){
        int ind = 0;
        HashMap<String, NonTerminal> overflow_map = new HashMap<>();
        for (Map.Entry<String,NonTerminal> curnont : non_terminal_map.entrySet()){
            int k = 0;
            for (Map.Entry<String,NonTerminal> nt : non_terminal_map.entrySet()){
                if (k >= ind) break;
                ArrayList<ArrayList<Component>> RemovedProd = new ArrayList<>();
                ArrayList<ArrayList<Component>> neededProd = new ArrayList<>();
                for (ArrayList<Component> prod : curnont.getValue().productions){
                    ArrayList<ArrayList<Component>> newprods = new ArrayList<>();
                    ArrayList<Component> newprod = new ArrayList<>();
                    boolean yes = false;
                    if (!prod.contains(nt.getValue())) continue;
                    for (Component cm : prod){
                        if (!yes) {
                            if (cm.equals(nt.getValue())) {
                                yes = true;
                                RemovedProd.add(prod);
                                for (ArrayList<Component> prodd : nt.getValue().productions) {
                                    ArrayList<Component> tmp = new ArrayList<>();
                                    tmp.addAll(newprod);
                                    tmp.addAll(prodd);
                                    newprods.add(tmp);
                                }
                            } else {
                                newprod.add(cm);
                            }
                        } else {
                            if (cm.equals(nt.getValue())) {
                                ArrayList<ArrayList<Component>> newtmp = new ArrayList<>(newprods);
                                newprods = new ArrayList<>();
                                for (ArrayList<Component> prodd : nt.getValue().productions) {
                                    ArrayList<ArrayList<Component>> tmp = new ArrayList<>(newtmp);
                                    for (ArrayList<Component> p : tmp) {
                                        ArrayList<Component> t = new ArrayList<>(p);
                                        t.addAll(prodd);
                                        newprods.add(t);
                                    }
                                }
                            }
                            else {
                                newprods.forEach(pro -> pro.add(cm));
                            }
                        }
                    }
                    neededProd.addAll(newprods);
                }
                curnont.getValue().productions.removeAll(RemovedProd);
                curnont.getValue().productions.addAll(neededProd);
                k++;
            }
            ArrayList<ArrayList<Component>> prods=curnont.getValue().productions;
            ArrayList<ArrayList<Component>> set=new ArrayList<>();
            NonTerminal newNT=new NonTerminal(curnont.getValue().name+"$");
            //get productions lead to left recursion
            ArrayList<ArrayList<Component>> newprods = new ArrayList<>();
            ArrayList<ArrayList<Component>> prodNewNt = new ArrayList<>();
            for (ArrayList<Component> prod : prods) {
                ArrayList<Component> tmp = new ArrayList<>(prod);
                if (prod.get(0).equals(curnont.getValue())) {
                    set.add(prod);
                    tmp.remove(0);
                    tmp.add(newNT);
                    newprods.add(tmp);
                } else {
                    tmp.add(newNT);
                    prodNewNt.add(tmp);
                }
            }
            prods.addAll(newprods);
            if (set.size()>0){
                //remove the prods that cause the left recursion
                prods.removeAll(set);
                newNT.productions.addAll(prodNewNt);
                overflow_map.put(newNT.name,newNT);
            }
            ind++;
        }
        non_terminal_map.putAll(overflow_map);
    }

    private void left_factor_for_all(){
        Queue<NonTerminal> queue = new LinkedList<>();
        for ( Map.Entry<String,NonTerminal> entry : non_terminal_map.entrySet() ){
            queue.add(entry.getValue());
        }

        left_factor(queue);
    }

    private void left_factor(Queue<NonTerminal> queue){
        HashMap<Component , ArrayList< ArrayList<Component> > > common = new HashMap<>();

        while (!queue.isEmpty()){
            NonTerminal source = queue.poll();

            for(int i=0 ; i<source.productions.size();i++){
                ArrayList<Component> production = source.productions.get(i);

                Component commonElem = production.get(0);

                ArrayList<Component> ComponentsNextToCommon = new ArrayList<>(production);
                ComponentsNextToCommon.remove(0);

                if(common.get(commonElem) == null){
                    ArrayList< ArrayList<Component> > temp = new ArrayList<>();
                    temp.add(ComponentsNextToCommon);
                    common.put(commonElem , temp);
                }else{
                    common.get(commonElem).add(ComponentsNextToCommon);
                }
            }


            int k = 1 ;

            for(Map.Entry<Component , ArrayList< ArrayList<Component>>> entry :  common.entrySet()){

                boolean epsilon = false;

                if(entry.getValue().size() > 1){
                    NonTerminal newNonTerminal = new NonTerminal(source.name+"'_" + k );
                    k++ ;
                    for(ArrayList<Component> arr : entry.getValue()){
                        if (arr.size() == 0){
                            epsilon = true;
                        }else {
                            newNonTerminal.productions.add(arr);
                        }
                    }

                    ArrayList<Integer> indexOfProductions = new ArrayList<>();
                    for (int i=0 ; i<source.productions.size();i++){
                        if(entry.getKey() == source.productions.get(i).get(0)){
                            indexOfProductions.add(i);
                        }
                    }

                    int counter = 0;
                    for (int i : indexOfProductions){
                        source.productions.remove(i-counter);
                        counter++;
                    }

                    ArrayList<Component> newProduction = new ArrayList<>();
                    newProduction.add(entry.getKey());
                    newProduction.add(newNonTerminal);

                    source.productions.add(newProduction);

                    if (epsilon){
                        newNonTerminal.epsilon_production = true;
                    }

                    queue.add(newNonTerminal);
                    non_terminal_map.put(newNonTerminal.name , newNonTerminal);
                }
            }

            common.clear();
        }
    }


    private void preFirstFollow (){
        for (Map.Entry<String,NonTerminal> nont : non_terminal_map.entrySet()){
            for (ArrayList<Component> prod : nont.getValue().productions){
                for (Component com : prod){
                    if (com.getClass().equals(NonTerminal.class)){
                        ((NonTerminal) com).mentioned_in_productions.add(new Pair(prod,nont.getValue()));
                    }
                }
            }
        }
    }


    public void parse_grammar (File file){
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        boolean st = true;
        String t = null;
        while (sc.hasNextLine() || (t != null && t.charAt(0) =='#')) {
            StringBuilder tmp = new StringBuilder((t == null || t.charAt(0) != '#') ? removeSpaces(sc.nextLine()) : t);
            t = null;
            while (sc.hasNextLine()){
                t = removeSpaces(sc.nextLine());
                if (t.charAt(0) != '#') {
                    tmp.append(t);
                } else break;
            }
            String s = tmp.toString();
            int in = s.indexOf('='),len = s.length();
            String sr = s.substring(2,in - 1);
            s =  s.substring(in + 2, len);
            String[] productions = s.split("\\|");
            NonTerminal src;
            if (non_terminal_map.containsKey(sr)){
                src = non_terminal_map.get(sr);
            } else {
                src = new NonTerminal(sr);
                non_terminal_map.put(sr,src);
            }
            if (st) {
                start_nonTerminal = src;
                st = false;
            }
            String[] prodword;
            ArrayList<Component> prod;
            for (String production : productions) {
                prodword = production.split(" ");
                prod = new ArrayList<>();
                for (String component : prodword) {
                    if (component.charAt(0) == '‘') { //Terminal
                        String term = component.substring(1, component.length() - 1);
                        Terminal cur;
                        if (!terminal_map.containsKey(term)) {
                            if ( term.equals("=") )
                                term = "assign" ;
                            cur = new Terminal(term);
                            terminal_map.put(term, cur);
                        } else {
                            cur = terminal_map.get(term);
                        }
                        prod.add(cur);
                    } else if (component.equals("\\L")) { //epsilon
                        src.epsilon_production = true;
                    } else {
                        NonTerminal nont;
                        if (non_terminal_map.containsKey(component))
                            nont = non_terminal_map.get(component);
                        else {
                            nont = new NonTerminal(component);
                            non_terminal_map.put(component, nont);
                        }
                        prod.add(nont);
                    }
                }
                src.productions.add(prod);
            }
        }
        terminal_map.put("$", new Terminal("$"));
    }

    private String removeSpaces(String s){
        StringBuilder res = new StringBuilder();
        for (int i = 0, len = s.length(); i < len;){
            if (s.charAt(i) == ' '){
                if (i != 0 && s.charAt(i + 1) != '|' && s.charAt(i - 1) != '|' && i != len - 1)
                    res.append(' ');
                while (i < s.length() && s.charAt(i) == ' ')
                    i++;
            } else if (s.charAt(i) == '‘'){
                res.append(s.charAt(i++));
                while (i < s.length() && s.charAt(i) != '’'){
                    if (s.charAt(i) != ' ') res.append(s.charAt(i));
                    i++;
                }
                res.append('’');
                i++;
            } else if (s.charAt(i) != ' '){
                res.append(s.charAt(i));
                i++;
            }
        }
        return res.toString();
    }

    private void compute_first_set_for_all(){
        for ( Map.Entry<String,NonTerminal> entry : non_terminal_map.entrySet() ){
            if(entry.getValue().first_set == null)
                computeFirstSet( entry.getValue() );
        }
    }

    private void computeFirstSet ( NonTerminal t ){

        t.first_set = new HashSet<>();

        int numOfProd = t.productions.size(); // get number of productions
        ArrayList<Component> production ;
        Component component ;


        for(int i=0 ; i < numOfProd ; i++){

            production = t.productions.get(i);
            component = production.get(0);

            //if first element in production is terminal add it to first set
            if(component instanceof Terminal){
                t.first_set.add((Terminal) component);
            }
            //if first element in production is nonterminal add its fists set to source nonterminal first set
            else {
                NonTerminal nonTerminal = (NonTerminal) component;

                //if the first set of non terminal does not compute yet then compute it frst
                if(nonTerminal.first_set == null)
                    computeFirstSet(nonTerminal);

                t.first_set.addAll(nonTerminal.first_set);

                //Check if nonterminal has epsilon production then add the first set of next
                // element in production to the first set of source
                int j ;
                for( j=1 ; j<production.size() && nonTerminal.epsilon_production ; j++){

                    if(production.get(j) instanceof  Terminal){
                        t.first_set.add((Terminal) production.get(j));
                        break;
                    }

                    nonTerminal = (NonTerminal) production.get(j) ;

                    //if the first set of non terminal does not compute yet then compute it frst
                    if(nonTerminal.first_set == null)
                        computeFirstSet(nonTerminal);

                    t.first_set.addAll(nonTerminal.first_set);
                }

                //if all nonterminal in productoin has epsilon production then add epsilon production to
                //the first set of source
                if (j == production.size() && nonTerminal.epsilon_production )
                    t.epsilon_production = true ;
            }
        }
    }

    private void compute_follow_set_for_all(){

        for ( Map.Entry<String,NonTerminal> entry : non_terminal_map.entrySet() ){
            if ( entry.getValue().follow_set == null )
                computeFollowSet( entry.getValue() );
        }

    }

    private void computeFollowSet(NonTerminal t) {

        t.follow_set = new HashSet<>();

        if ( t == start_nonTerminal )
            t.follow_set.add( terminal_map.get("$") );

        for (Pair x : t.mentioned_in_productions) {

            ArrayList<Component> production = x.production;

            int counter = 0;

            Component temp = production.get(counter);

            while (temp.getClass() != NonTerminal.class || temp != t) {
                counter++;
                temp = production.get(counter);
            }

            boolean finish = false;

            counter++;

            for (; counter < production.size(); counter++) {

                Component temp_2 = production.get(counter);

                if (temp_2.getClass() == Terminal.class) {

                    Terminal temp_3 = (Terminal) temp_2;

                    t.follow_set.add(temp_3);

                    finish = true;

                    break;

                } else if (temp_2.getClass() == NonTerminal.class) {

                    NonTerminal temp_3 = (NonTerminal) temp_2;

                    t.follow_set.addAll(temp_3.first_set);

                    if (!temp_3.epsilon_production) {
                        finish = true;
                        break;
                    }

                }

            }
            if (!finish){
                if ( x.source.follow_set == null )
                    computeFollowSet(x.source);
                t.follow_set.addAll(x.source.follow_set);
            }

        }

    }

    private boolean computeParseTable(){

        parsing_table = new ArrayList[non_terminal_map.size()][terminal_map.size()];

        int counter = 0 ;

        for (Map.Entry< String , NonTerminal > entry : non_terminal_map.entrySet() ){
            rows_map.put( entry.getValue() , counter );
            counter++;
        }

        counter = 0 ;

        for (Map.Entry< String , Terminal > entry : terminal_map.entrySet() ){
            columns_map.put( entry.getValue() , counter );
            counter++;
        }

        // setting parse table entries for each non terminal (row)

        for (Map.Entry< String , NonTerminal > entry : non_terminal_map.entrySet() ){

            int row = rows_map.get( entry.getValue() );

            // setting parsing table entries for first set of current non terminal

            for ( ArrayList<Component> production : entry.getValue().productions ){

                Component temp = production.get(0);

                if ( temp.getClass() == NonTerminal.class ){

                    NonTerminal temp_2 = (NonTerminal) temp ;

                    Iterator<Terminal> itr = temp_2.first_set.iterator();

                    int column ;

                    while ( itr.hasNext() ){
                        column = columns_map.get( itr.next() );
                        if ( parsing_table[row][column] == null )
                            parsing_table[row][column] = production ;
                        else
                            return false ;
                    }

                }
                else if ( temp.getClass() == Terminal.class ){
                    int column = columns_map.get( temp );
                    if ( parsing_table[row][column] == null )
                        parsing_table[row][column] = production ;
                    else
                        return false ;
                }

            }

            Iterator<Terminal> itr = entry.getValue().follow_set.iterator();

            // setting parsing table entries for sync and epsilon productions

            if ( entry.getValue().epsilon_production ){
                while (itr.hasNext()){
                    int column = columns_map.get(itr.next());
                    if ( parsing_table[row][column] == null )
                        parsing_table[row][column] = dummy_for_epsilon ;
                    else
                        return false ;
                }
            }
            else {
                while (itr.hasNext()){
                    int column = columns_map.get(itr.next());
                    if ( parsing_table[row][column] == null )
                        parsing_table[row][column] = dummy_for_sync ;
                }
            }

        }
        return true ;
    }

    private void process_input ()throws Exception{
        /*
         * 1- push dollar_sign into the stack
         * 2-push start of non terminal
         * 3-while(stack not empty)
         *   -get nexttoken
         *   -create component topOfstack=stack.top();
         *   -check if topOfstack isTerminal
         *     *check whether topOfstack.name==nexttoken then stack.pop();
         *     *else Report Error and in next loop DON'T TAKE NEXT TOKEN
         *   - if topofstack not terminal
         *     *get row index of topofstack
         *     *create terminal=terminalmap.get(nexttoken);
         *     *get column index of nexttoken
         *     *get the component tostack to place into the stack from parsing table
         *        check if :
         *          --if tostack =dummy_sync
         *            >Report Error
         *            >POP stack
         *            >DON'T TAKE THE NEXTTOKEN
         *          --tostack= null
         *            >Report ERROR
         *            >get next token
         *          -- else pop the stack AND take the productions of the component aqnd push from last to first
         * */
        LexicalAnalyzerGenerator Lexicalgenerator=new LexicalAnalyzerGenerator();
        BufferedWriter bw = new BufferedWriter(new FileWriter("output_grammar.txt"));
        boolean takenext=true;
        Stack<Component>stack=new Stack<>();
        stack.add(terminal_map.get("$"));
        stack.add(start_nonTerminal);
        String nexttoken="";
        while (!stack.isEmpty()){
            if (takenext)
                nexttoken=Lexicalgenerator.getNextToken();
            if ( nexttoken == null )
                break;
            Component topOfstack=stack.peek();
            if (topOfstack.getClass()==Terminal.class){
                if (topOfstack.name.equals(nexttoken)){
                    bw.write("MATCHING ' " + nexttoken + " ' ...\n\n");
                    stack.pop();
                    takenext=true;
                }
                else {
                    bw.write("ERROR ... INSERT " + topOfstack.name + "\n\n" );
                    takenext=false;
                    stack.pop();
                }
            }
            else if ( topOfstack.getClass() == NonTerminal.class ) {
                Integer row=rows_map.get(topOfstack);
                Terminal terminal=terminal_map.get(nexttoken);
                Integer column=columns_map.get(terminal);
                ArrayList<Component> tostackList=parsing_table[row][column];
                if (tostackList==null){
                    bw.write("ERROR ...\n\n");
                    takenext=true;
                }
                else if (tostackList==dummy_for_sync){
                    bw.write("ERROR ...\n\n");
                    stack.pop();
                    takenext=false;
                }
                else
                {    takenext=false;
                    stack.pop();
                    if (tostackList==dummy_for_epsilon){
                        bw.write(topOfstack.name+" -> "+"epsilon\n\n");
                        continue;
                    }
                    for (int i=tostackList.size()-1;i>=0;i--)
                        stack.push( tostackList.get(i) );

                    bw.write(topOfstack.name+" -> ");

                    for (Component component : tostackList)
                        bw.write(component.name + " ");

                    bw.write("\n\n");
                }
            }
        }
        bw.close();
    }
}