package parser_generator ;


import java.util.ArrayList;

public class Pair {

    ArrayList<Component> production ;

    NonTerminal source ;

    public Pair(ArrayList<Component> production, NonTerminal source) {
        this.production = production;
        this.source = source;
    }
}
