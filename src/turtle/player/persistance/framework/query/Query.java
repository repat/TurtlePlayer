package turtle.player.persistance.framework.query;

import android.database.Cursor;
import turtle.player.persistance.framework.db.Database;
import turtle.player.persistance.framework.filter.Filter;
import turtle.player.persistance.framework.filter.FilterVisitor;
import turtle.player.persistance.framework.selector.QuerySelector;
import turtle.player.persistance.source.sql.Sql;

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

public interface Query<R, I, C> extends FilterVisitor<R>
{
	R get(QuerySelector<R, I, C> querySelector, Filter<R> filter);

	I execute(Database<R, C, ?> db, QuerySelector<R, I, C> querySelector, Filter<R> filter);
}
