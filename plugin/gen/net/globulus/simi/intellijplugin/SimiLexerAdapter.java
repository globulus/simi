import com.intellij.lexer.FlexAdapter;

public class SimiLexerAdapter extends FlexAdapter {
    public SimiLexerAdapter() {
        super(new SimiLexer());
    }
}
