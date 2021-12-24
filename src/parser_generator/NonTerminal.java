package parser_generator ;


import java.util.ArrayList;
import java.util.HashSet;

public class NonTerminal extends Component{

    boolean epsilon_production ;

    ArrayList< ArrayList<Component> > productions ;

    HashSet<Terminal> first_set ;

    HashSet<Terminal> follow_set ;

    ArrayList< Pair > mentioned_in_productions ;

    public NonTerminal(String name) {
        super(name);
        productions = new ArrayList<>();
        epsilon_production = false ;
        mentioned_in_productions = new ArrayList<>();
    }


}
