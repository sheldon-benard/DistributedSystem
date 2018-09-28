package Server.Common;

import java.util.*;

public class Parser {

    public static Vector<String> parse(String input)
    {
        if (input == null || input.length() == 0)
            return null;

        String command;

        if (input.charAt(0) == '[' && input.charAt(input.length() - 1) == ']')
            command = input.substring(1,input.length() - 1);
        else
            command = input;

        Vector<String> arguments = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(command,",");
        String argument = "";
        while (tokenizer.hasMoreTokens())
        {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }
}
