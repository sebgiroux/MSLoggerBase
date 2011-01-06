package uk.org.smithfamily.msdisp.parser;

import java.util.ArrayList;
import java.util.List;

import java.text.ParseException;

public class Tokenizer
{
	enum type
	{
		Tdir, Tid, Tnum, Tstr, Tblk, Texp, Tcol, Tgarbage
	};

	class Token
	{
		String str;
		type typ;
		double num;

		Token(String s, type t, double d)
		{
			str = s;
			typ = t;
			num = d;
		}
	};

	List<Token> tokens;

	int parse(String line) throws ParseException
	{
		tokens = new ArrayList<Token>();

		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			if (c == ';')
			{
				break;
			}
			if (Character.isWhitespace(c))
			{
				continue;
			}
			if (c == ',' || c == '=')
			{
				continue;
			}
			if (c == '#')
			{
				tokens.add(new Token("#", type.Tdir, 0));
				continue;
			}
			if (c == ':')
			{ // Syntax
				tokens.add(new Token(":", type.Tcol, 0));
				continue;
			}

			if (Character.isLetterOrDigit(c))
			{ // Parse alpha-numeric id.
				StringBuffer tok = new StringBuffer();
				int j = 0;
				for (j = i; Character.isLetterOrDigit(line.charAt(j))
						|| line.charAt(j) == '_'; j++)
				{
					tok.append(line.charAt(j));
				}
				tokens.add(new Token(tok.toString(), type.Tid, 0));
				i = j;
				continue;
			}

			if (Character.isDigit(c) || c == '-')
			{
				StringBuffer tok = new StringBuffer();
				tok.append(c);
				int j = 0;
				for (j = i + 1; Character.isDigit(line.charAt(j)) || c == '.'; j++)
				{
					tok.append(line.charAt(j));
				}
				tokens.add(new Token(tok.toString(), type.Tnum, Double
						.valueOf(tok.toString())));
				i = j;
				continue;
			}

			if (c == '"')
			{ // Quoted string, quotes removed.
				StringBuffer tok = new StringBuffer();
				int j = i + 1;
				for (; j < line.length() && line.charAt(j) != '"'; j++)
				{
					tok.append(line.charAt(j));
				}
				if (line.charAt(j) != '"')
					throw new ParseException("Closing quote not found.", j);
				i++;
				tokens.add(new Token(tok.toString(), type.Tstr, 0));
				continue;
			}
			if (c == '[')
			{ // Block identifier
				StringBuffer tok = new StringBuffer();
				tok.append(c);
				int j;
				for (j = i; j < line.length() - 1 && line.charAt(j) != ']'; j++)
				{
					tok.append(line.charAt(j));
				}
				tok.append(line.charAt(j));
				tokens.add(new Token(tok.toString(), type.Tblk, 0));
				continue;
			}
			if (c == '{')
			{ // Block identifier
				StringBuffer tok = new StringBuffer();
				tok.append(c);
				int j;
				for (j = i; j < line.length() - 1 && line.charAt(j) != '}'; j++)
				{
					tok.append(line.charAt(j));
				}
				tok.append(line.charAt(j));
				tokens.add(new Token(tok.toString(), type.Texp, 0));
				continue;
			}

		}
		return tokens.size();
	}

	double v(int idx)
	{
		return idx < size() ? tokens.get(idx).num : 0.0;
	}

	int i(int idx)
	{
		return (int) (idx < size() ? tokens.get(idx).num : 0);
	}

	type t(int idx)
	{
		return idx < size() ? tokens.get(idx).typ : type.Tgarbage;
	}

	String stripped(int idx)
	{
		return "";
	}

	int size()
	{
		return tokens.size();
	}

	boolean eq(String s, int idx)
	{
		return s.equals(tokens.get(idx).str);
	}

	boolean match(String s, int idx)
	{
		return tokens.get(idx).str.startsWith(s);
	}

}
