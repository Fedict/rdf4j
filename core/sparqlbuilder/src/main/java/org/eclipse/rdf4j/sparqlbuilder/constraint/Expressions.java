/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.ABS;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.BNODE;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.BOUND;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.CEIL;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.COALESCE;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.CONCAT;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.REGEX;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

/**
 * A class with static methods to create SPARQL expressions. Obviously there's some more flushing out TODO still
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps"> SPARQL Function Definitions</a>
 */
public class Expressions {
	private Expressions() {
	}

	/**
	 * <pre>{@code ABS(operand)}</pre>
	 *
	 * @param operand the argument to the absolute value function
	 * @return an ABS() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-abs"> SPARQL ABS Function</a>
	 */
	public static Expression<?> abs(Number operand) {
		return abs(Rdf.literalOf(operand));
	}

	/**
	 * <pre>{@code ABS(operand}</pre>
	 *
	 * @param operand the argument to the absolute value function
	 * @return an ABS() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-abs"> SPARQL ABS Function</a>
	 */
	public static Expression<?> abs(Operand operand) {
		return function(ABS, operand);
	}

	/**
	 * <pre>{@code BNODE()}</pre>
	 *
	 * @return a no-arg BNODE() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-bnode"> SPARQL BNODE Function</a>
	 */
	public static Expression<?> bnode() {
		return function(BNODE);
	}

	/**
	 * <pre>{@code BNODE(operand)}</pre>
	 *
	 * @param literal the RDF literal argument to the function
	 * @return a BNODE() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-bnode"> SPARQL BNODE Function</a>
	 */
	public static Expression<?> bnode(RdfLiteral<?> literal) {
		return function(BNODE, literal);
	}

	/**
	 * <pre>{@code BNODE(operand)}</pre>
	 *
	 * @param literal the String literal argument to the function
	 * @return a BNODE() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-bnode"> SPARQL BNODE Function</a>
	 */
	public static Expression<?> bnode(String literal) {
		return function(BNODE, Rdf.literalOf(literal));
	}

	/**
	 * <pre>{@code BOUND(operand)}</pre>
	 *
	 * @param var the SPARQL variable argument to the function
	 * @return a BOUND() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-bound"> SPARQL BOUND Function</a>
	 */
	public static Expression<?> bound(Variable var) {
		return function(BOUND, var);
	}

	/**
	 * <pre>{@code CEIL(operand)}</pre>
	 *
	 * @param operand the argument to the function
	 * @return a CEIL() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-ceil"> SPARQL CEIL Function</a>
	 */
	public static Expression<?> ceil(Operand operand) {
		return function(CEIL, operand);
	}

	/**
	 * <pre>{@code COALESCE(operand1, operand2, ... , operandN)}</pre>
	 *
	 * @param operands the arguments to the function
	 * @return a COALESCE() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-coalesce"> SPARQL COALESCE Function</a>
	 */
	public static Expression<?> coalesce(Operand... operands) {
		return function(COALESCE, operands);
	}

	/**
	 * <pre>{@code CONCAT(operand1, operand2, ... , operandN)}</pre>
	 *
	 * @param operands the arguments to the function
	 * @return a CONCAT() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-concat"> SPARQL CONCAT Function</a>
	 */
	public static Expression<?> concat(Operand... operands) {
		return function(CONCAT, operands);
	}

	/**
	 * <pre>{@code REGEX(testString, pattern)}</pre> 
	 *
	 * @param testString the text to match against
	 * @param pattern    the regex pattern to match
	 * @return a REGEX() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-regex"> SPARQL REGEX Function</a>
	 */
	public static Expression<?> regex(Operand testString, String pattern) {
		return regex(testString, Rdf.literalOf(pattern));
	}

	/**
	 * <pre>{@code REGEX(testString, pattern, flags)}</pre> 
	 *
	 * @param testString the text to match against
	 * @param pattern    the regular expression pattern to match
	 * @param flags      flags to specify matching options
	 * @return a REGEX() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-regex"> SPARQL REGEX Function</a>
	 */
	public static Expression<?> regex(Operand testString, String pattern, String flags) {
		return regex(testString, Rdf.literalOf(pattern), Rdf.literalOf(flags));
	}

	/**
	 * <pre>{@code REGEX(testString, pattern)}</pre>
	 *
	 * @param testString the text to match against
	 * @param pattern    the regex pattern to match
	 * @return a REGEX() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-regex"> SPARQL REGEX Function</a>
	 */
	public static Expression<?> regex(Operand testString, Operand pattern) {
		return function(REGEX, testString, pattern);
	}

	/**
	 * <pre>{@code REGEX(testString, pattern, flags)}</pre> 
	 *
	 * @param testString the text to match against
	 * @param pattern    the regular expression pattern to match
	 * @param flags      flags to specify matching options
	 * @return a REGEX() function
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-regex"> SPARQL REGEX Function</a>
	 */
	public static Expression<?> regex(Operand testString, Operand pattern, Operand flags) {
		return function(REGEX, testString, pattern, flags);
	}

	/**
	 * <pre>{@code STR(literal)}</pre> or <pre>{@code STR(iri)}</pre>
	 *
	 * @param operand the arg to convert to a string
	 * @return a {@code STR()} function
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#func-str"> SPARQL STR Function</a>
	 */
	public static Expression<?> str(Operand operand) {
		return function(SparqlFunction.STRING, operand);
	}

	public static Expression<?> custom(Iri functionIri, Operand... operands) {
		return new CustomFunction(functionIri).addOperand(operands);
	}

	public static Expression<?> custom(IRI functionIri, Operand... operands) {
		return new CustomFunction(functionIri).addOperand(operands);
	}

	/**
	 * <pre>{@code operand IN (expression1, expression2...)}</pre>
	 *
	 * @param searchTerm
	 * @param expressions
	 * @return an {@code IN} function
	 * @see <a href="https://www.w3.org/TR/sparql11-query/#func-in">SPARQL IN Function</a>
	 */
	public static Expression<?> in(Operand searchTerm, Operand... expressions) {
		return new In(searchTerm, expressions);
	}

	/**
	 * <pre>{@code operand NOT IN (expression1, expression2...)}</pre>
	 *
	 * @param searchTerm
	 * @param expressions
	 * @return an {@code NOT IN} function
	 * @see <a href="https://www.w3.org/TR/sparql11-query/#func-not-in">SPARQL NOT IN Function</a>
	 */
	public static Expression<?> notIn(Operand searchTerm, Operand... expressions) {
		return new In(searchTerm, false, expressions);
	}

	// ... etc...

	/**
	 * Too lazy at the moment. Make the rest of the functions this way for now.
	 *
	 * @param function a SPARQL Function
	 * @param operands arguments to the function
	 * @return a function object of the given <pre>{@code function}</pre> type and <pre>{@code operands}</pre>
	 */
	public static Expression<?> function(SparqlFunction function, Operand... operands) {
		return new Function(function).addOperand(operands);
	}

	/**
	 * <pre>{@code !operand}</pre>
	 *
	 * @param operand argument to the function
	 * @return logical not operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> not(Operand operand) {
		return unaryExpression(UnaryOperator.NOT, operand);
	}

	/**
	 * <pre>{@code +operand}</pre>
	 *
	 * @param operand argument to the function
	 * @return unary plus operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> plus(Operand operand) {
		return unaryExpression(UnaryOperator.UNARY_PLUS, operand);
	}

	/**
	 * <pre>{@code -operand}</pre>
	 *
	 * @param operand argument to the function
	 * @return unary minus operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> minus(Operand operand) {
		return unaryExpression(UnaryOperator.UNARY_MINUS, operand);
	}

	private static UnaryOperation unaryExpression(UnaryOperator operator, Operand operand) {
		return new UnaryOperation(operator).addOperand(operand);
	}

	/**
	 * <pre>{@code left = right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical equals operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> equals(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.EQUALS, left, right);
	}

	/**
	 * <pre>{@code left != right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical not equals operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> notEquals(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.NOT_EQUALS, left, right);
	}

	public static Expression<?> notEquals(Variable left, RdfValue right) {
		return binaryExpression(BinaryOperator.NOT_EQUALS, left, right);
	}

	public static Expression<?> notEquals(Variable left, IRI right) {
		return binaryExpression(BinaryOperator.NOT_EQUALS, left, iri(right));
	}

	/**
	 * <pre>{@code left > right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical greater than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> gt(Number left, Number right) {
		return binaryExpression(BinaryOperator.GREATER_THAN, Rdf.literalOf(left), Rdf.literalOf(right));
	}

	/**
	 * <pre>{@code left > right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical greater than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> gt(Number left, Operand right) {
		return binaryExpression(BinaryOperator.GREATER_THAN, Rdf.literalOf(left), right);
	}

	/**
	 * <pre>{@code left > right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical greater than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> gt(Operand left, Number right) {
		return binaryExpression(BinaryOperator.GREATER_THAN, left, Rdf.literalOf(right));
	}

	/**
	 * <pre>{@code left > right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical greater than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> gt(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.GREATER_THAN, left, right);
	}

	/**
	 * <pre>{@code left >= right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical greater than or equals operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> gte(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.GREATER_THAN_EQUALS, left, right);
	}

	/**
	 * <pre>{@code left < right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical less than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> lt(Number left, Number right) {
		return binaryExpression(BinaryOperator.LESS_THAN, Rdf.literalOf(left), Rdf.literalOf(right));
	}

	/**
	 * <pre>{@code left < right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical less than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> lt(Number left, Operand right) {
		return binaryExpression(BinaryOperator.LESS_THAN, Rdf.literalOf(left), right);
	}

	/**
	 * <pre>{@code left < right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical less than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> lt(Operand left, Number right) {
		return binaryExpression(BinaryOperator.LESS_THAN, left, Rdf.literalOf(right));
	}

	/**
	 * <pre>{@code left < right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical less than operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> lt(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.LESS_THAN, left, right);
	}

	/**
	 * <pre>{@code left <= right}</pre>
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return logical less than or equals operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> lte(Operand left, Operand right) {
		return binaryExpression(BinaryOperator.LESS_THAN_EQUALS, left, right);
	}

	private static BinaryOperation binaryExpression(BinaryOperator operator, Operand op1, Operand op2) {
		BinaryOperation op = new BinaryOperation(operator);

		op.addOperand(op1).addOperand(op2);

		return op;
	}

	/**
	 * <pre>{@code operand1 && operand2 && ... operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return logical and operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> and(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.AND, operands);
	}

	/**
	 * <pre>{@code operand1 || operand2 || ... || operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return logical or operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> or(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.OR, operands);
	}

	/**
	 * <pre>{@code operand1 + operand2 + ... + operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return arithmetic addition operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> add(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.ADD, operands);
	}

	/**
	 * <pre>{@code operand1 - operand2 - ... - operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return arithmetic subtraction operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> subtract(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.SUBTRACT, operands);
	}

	/**
	 * <pre>{@code operand1 * operand2 * ... * operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return arithmetic multiplication operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> multiply(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.MULTIPLY, operands);
	}

	/**
	 * <pre>{@code operand1 / operand2 / ... / operandN}</pre>
	 *
	 * @param operands the arguments
	 * @return arithmetic division operation
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">SPARQL Operators</a>
	 */
	public static Expression<?> divide(Operand... operands) {
		return connectiveExpression(ConnectiveOperator.DIVIDE, operands);
	}

	private static ConnectiveOperation connectiveExpression(ConnectiveOperator operator, Operand... operands) {
		ConnectiveOperation op = new ConnectiveOperation(operator);

		for (Operand operand : operands) {
			op.addOperand(operand);
		}

		return op;
	}

	/**
	 * Aggregates
	 */

	/**
	 * <pre>{@code avg(...)}</pre>
	 *
	 * @param operand the expression to average
	 * @return an avg aggregate function
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#aggregates"> SPARQL aggregates</a>
	 */
	public static Aggregate avg(Operand operand) {
		return new Aggregate(SparqlAggregate.AVG).addOperand(operand);
	}

	/**
	 * <pre>{@code count()}</pre>
	 *
	 * @param operand the expression to count
	 * @return a count aggregate
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#aggregates"> SPARQL aggregates</a>
	 */
	public static Aggregate count(Operand operand) {
		return new Aggregate(SparqlAggregate.COUNT).addOperand(operand);
	}

	public static Aggregate countAll() {
		return new Aggregate(SparqlAggregate.COUNT).countAll();
	}

	public static Aggregate group_concat(Operand... operands) {
		return new Aggregate(SparqlAggregate.GROUP_CONCAT).addOperand(operands);
	}

	public static Aggregate group_concat(String separator, Operand... operands) {
		return new Aggregate(SparqlAggregate.GROUP_CONCAT).addOperand(operands).separator(separator);
	}

	public static Aggregate max(Operand operand) {
		return new Aggregate(SparqlAggregate.MAX).addOperand(operand);
	}

	public static Aggregate min(Operand operand) {
		return new Aggregate(SparqlAggregate.MIN).addOperand(operand);
	}

	public static Aggregate sample(Operand operand) {
		return new Aggregate(SparqlAggregate.SAMPLE).addOperand(operand);
	}

	public static Aggregate sum(Operand operand) {
		return new Aggregate(SparqlAggregate.SUM).addOperand(operand);
	}

	public static Bind bind(Assignable exp, Variable var) {
		return new Bind(exp, var);
	}

	public static Expression<?> notIn(Variable var, RdfValue... options) {
		return new NotIn(var, options);
	}

	public static Expression<?> notIn(Variable var, IRI... options) {
		return notIn(var, parseIRIOptionsToRDFValueVarargs(options));
	}

	public static Expression<?> in(Variable var, RdfValue... options) {
		return new In(var, options);
	}

	public static Expression<?> in(Variable var, IRI... options) {
		return in(var, parseIRIOptionsToRDFValueVarargs(options));
	}

	public static Expression<?> strdt(Operand lexicalForm, Operand datatype) {
		return function(SparqlFunction.STRDT, lexicalForm, datatype);
	}

	public static Expression<?> strlen(Operand operand) {
		return function(SparqlFunction.STRLEN, operand);
	}

	public static Expression<?> isBlank(Variable var) {
		return function(SparqlFunction.IS_BLANK, var);
	}

	public static Expression<?> datatype(Variable var) {
		return function(SparqlFunction.DATATYPE, var);
	}

	public static Expression<?> iff(Operand testExp, Operand thenExp, Operand elseExp) {
		return function(SparqlFunction.IF, testExp, thenExp, elseExp);
	}

	/**
	 * Parses IRI... options to RdfValue... options to give more flexibility in expressions use
	 *
	 * @param options options as IRIs
	 * @return options as RDFValues
	 */
	private static RdfValue[] parseIRIOptionsToRDFValueVarargs(IRI... options) {
		List<RdfValue> rdfValueOptions = new ArrayList<>();
		for (IRI option : options) {
			rdfValueOptions.add(iri(option));
		}
		return rdfValueOptions.toArray(new RdfValue[0]);
	}
}
