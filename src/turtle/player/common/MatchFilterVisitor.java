package turtle.player.common;

import turtle.player.persistance.framework.filter.*;
import turtle.player.persistance.source.relational.FieldPersistable;
import turtle.player.persistance.source.relational.fieldtype.FieldPersistableAsDouble;
import turtle.player.persistance.source.relational.fieldtype.FieldPersistableAsInteger;
import turtle.player.persistance.source.relational.fieldtype.FieldPersistableAsString;

/**
 * TURTLE PLAYER
 * <p/>
 * Licensed under MIT & GPL
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * More Information @ www.turtle-player.co.uk
 *
 * @author Simon Honegger (Hoene84)
 */

public class MatchFilterVisitor<RESULT, TARGET> extends FilterVisitorGenerified<TARGET, RESULT, Object, Boolean>
{
	private final RESULT instance;

	public MatchFilterVisitor(RESULT instance)
	{
		this.instance = instance;
	}

	@Override
	public Boolean visit(final FieldFilter<TARGET, RESULT, Object> fieldFilter,
								FieldPersistable<RESULT, Object> field)
	{

		return field.accept(fieldFilter.new FieldVisitorField<Boolean>()
		{
			@Override
			public Boolean visit(FieldPersistableAsString<RESULT> field,
										String filterValue)
			{
				return matchField(filterValue, field.get(instance), fieldFilter.getOperator());
			}

			@Override
			public Boolean visit(FieldPersistableAsDouble<RESULT> field,
										Double filterValue)
			{
				return matchField(filterValue, field.get(instance), fieldFilter.getOperator());
			}

			@Override
			public Boolean visit(FieldPersistableAsInteger<RESULT> field,
										Integer filterValue)
			{
				return matchField(filterValue, field.get(instance), fieldFilter.getOperator());
			}
		});
	}

	public Boolean visit(FilterSet<TARGET> filterSet)
	{
		for(Filter<TARGET> filter : filterSet.getFilters())
		{
			if(!filter.accept(this))
			{
				return false;
			}
		}
		return true;
	}

	public Boolean visit(NotFilter<TARGET> notFilter)
	{
		return !notFilter.accept(this);
	}

	private boolean matchField(String filterValue, String fieldValue, Operator operator){

		Boolean nullCompare = matchFieldNull(filterValue, fieldValue, operator);
		if(nullCompare != null)
		{
			return nullCompare;
		}

		switch (operator)
		{
			case EQ:
				return fieldValue.equals(filterValue);
			case NEQ:
				return !fieldValue.equals(filterValue);
			case GT:
				return fieldValue.compareTo(filterValue) > 0;
			case LE:
				return fieldValue.compareTo(filterValue) <= 0;
			case GE:
				return fieldValue.compareTo(filterValue) >= 0;
			case LIKE:
				return fieldValue.contains(filterValue);
			case NOT_LIKE:
				return !fieldValue.contains(filterValue);
			case LT:
				return fieldValue.compareTo(filterValue) < 0;
		}
		throw new RuntimeException("Operator " + operator + " is not supported");
	}

	private boolean matchField(Double filterValue, Double fieldValue, Operator operator){
		Boolean nullCompare = matchFieldNull(filterValue, fieldValue, operator);
		if(nullCompare != null)
		{
			return nullCompare;
		}

		switch (operator)
		{
			case EQ:
				return fieldValue.equals(filterValue);
			case GT:
				return fieldValue.compareTo(filterValue) > 0;
			case LE:
				return fieldValue.compareTo(filterValue) <= 0;
			case GE:
				return fieldValue.compareTo(filterValue) >= 0;
			case LIKE:
				return String.valueOf(fieldValue).contains(String.valueOf(filterValue));
			case LT:
				return fieldValue.compareTo(filterValue) < 0;
			case NOT_LIKE:
				return !String.valueOf(fieldValue).contains(String.valueOf(filterValue));
			case NEQ:
				return !fieldValue.equals(filterValue);
		}
		throw new RuntimeException("Operator " + operator + " is not supported");
	}

	private boolean matchField(Integer filterValue, Integer fieldValue, Operator operator){
		Boolean nullCompare = matchFieldNull(filterValue, fieldValue, operator);
		if(nullCompare != null)
		{
			return nullCompare;
		}

		switch (operator)
		{
			case EQ:
				return fieldValue.equals(filterValue);
			case NEQ:
				return !fieldValue.equals(filterValue);
			case GT:
				return fieldValue.compareTo(filterValue) > 0;
			case LE:
				return fieldValue.compareTo(filterValue) <= 0;
			case GE:
				return fieldValue.compareTo(filterValue) >= 0;
			case LIKE:
				return String.valueOf(fieldValue).contains(String.valueOf(filterValue));
			case NOT_LIKE:
				return !String.valueOf(fieldValue).contains(String.valueOf(filterValue));
			case LT:
				return fieldValue.compareTo(filterValue) < 0;
		}
		throw new RuntimeException("Operator " + operator + " is not supported");
	}

	/**
	 * TODO Refactor, seems to be not used, implementation is bullshit
	 */
	private Boolean matchFieldNull(Object filterValue, Object fieldValue, Operator operator){
		if(filterValue != null || fieldValue != null) return null;

		switch (operator)
		{
			case NEQ:
				return filterValue != fieldValue;
			case EQ:
				return filterValue == fieldValue;
			case GT:
				return filterValue != fieldValue && fieldValue != null;
			case LE:
				return filterValue == fieldValue || fieldValue == null;
			case GE:
				return fieldValue == filterValue || fieldValue != null;
			case LIKE:
				return fieldValue == filterValue || filterValue == null;
			case NOT_LIKE:
				return fieldValue != filterValue || filterValue == null;
			case LT:
				return filterValue != fieldValue && fieldValue == null;
		}
		throw new RuntimeException("Operator " + operator + " is not supported");
	}
}
