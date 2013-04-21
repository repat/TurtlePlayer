/**
 *
 * TURTLE PLAYER
 *
 * Licensed under MIT & GPL
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * More Information @ www.turtle-player.co.uk
 *
 * @author Simon Honegger (Hoene84)
 */

package turtle.player.view;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import turtle.player.Player;
import turtle.player.R;
import turtle.player.common.MatchFilterVisitor;
import turtle.player.model.*;
import turtle.player.persistance.framework.filter.*;
import turtle.player.persistance.turtle.db.TurtleDatabase;
import turtle.player.persistance.turtle.db.structure.Tables;
import turtle.player.presentation.InstanceFormatter;
import turtle.player.util.DefaultAdapter;

import java.util.*;

public abstract class FileChooser implements TurtleDatabase.DbObserver
{

	public enum Mode
	{
		Album(R.id.albumButton, R.drawable.album48, R.drawable.album48_active),
		Artist(R.id.artistButton, R.drawable.artist48, R.drawable.artist48_active),
		Track(R.id.trackButton, R.drawable.track48, R.drawable.track48_active),
		Genre(R.id.genreButton, R.drawable.genre48, R.drawable.genre48_active),
		Dir(R.id.dirButton, R.drawable.dir48, R.drawable.dir48_active);

		private Mode(int buttonId,
			  int drawable,
			  int drawableActive)
		{
			this.drawable = drawable;
			this.drawableActive = drawableActive;
			this.buttonId = buttonId;
		}

		private final int drawable;
		private final int drawableActive;
		private final int buttonId;
	}

	private Mode currMode;
	private final TurtleDatabase database;
	private final Player listActivity;
	final DefaultAdapter<Instance> listAdapter;
	final ArrayAdapter<Filter<Tables.Tracks>> filterListAdapter;

	ListView filterList = null;

	private Set<Filter<Tables.Tracks>> filters = new HashSet<Filter<Tables.Tracks>>();
	private Filter<Tables.Dirs> dirFilter = null;
	private Map<Mode, Filter<Track>> filtersAddWithMode = new HashMap<Mode, Filter<Track>>();

	public FileChooser(Mode currMode,
							 TurtleDatabase db,
							 Player listActivity)
	{
		this.currMode = currMode;
		this.database = db;
		this.listActivity = listActivity;

		filterList = (ListView) listActivity.findViewById (R.id.filterlist);
		filterListAdapter = new FilterListAdapter(listActivity.getApplicationContext(), new ArrayList<Filter<Tables.Tracks>>(filters))
		{
			@Override
			protected void removeFilter(final Filter<Tables.Tracks> filter)
			{
				filters.remove(filter);
				filterList.post(new Runnable()
				{
					public void run()
					{
						filterListAdapter.remove(filter);
					}
				});
				update();
			}

			@Override
			protected void chooseFilter(Filter filter)
			{
				filterChoosen(filter);
			}
		};

		filterList.setAdapter(filterListAdapter);

		listAdapter = new DefaultAdapter<Instance>(
				  listActivity.getApplicationContext(),
				  new ArrayList<Instance>(),
				  listActivity,
				  false,
				  InstanceFormatter.SHORT);

		listActivity.setListAdapter(listAdapter);

		change(currMode, null);

		init();
	}

	private void init()
	{
		database.addObserver(this);

		for (final Mode currMode : Mode.values())
		{
			getButton(currMode).setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					filters.clear();
					filterListAdapter.clear();
					filtersAddWithMode.clear();
					change(currMode, null);
				}
			});
		}
	}

	private Filter<Tables.Tracks> getFilter()
	{
		return new FilterSet<Tables.Tracks>(filters);
	}

	/**
	 * @param selection
	 * @return null if no track was selected, track if trak was selected
	 */
	public Track choose(Instance selection)
	{

		return selection.accept(new InstanceVisitor<Track>()
		{
			public Track visit(Track track)
			{
				return track;
			}

			public Track visit(TrackDigest track)
			{
				Filter<Tables.Tracks> trackFilter = new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.TITLE, Operator.EQ, track.getName());
				return database.getTracks(new FilterSet<Tables.Tracks>(getFilter(), trackFilter)).iterator().next();
			}

			public Track visit(Album album)
			{
				Filter filter = new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.ALBUM, Operator.EQ, album.getId());
				change(Mode.Track, filter);
				return null;
			}

			public Track visit(Genre genre)
			{
				Filter filter = new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.GENRE, Operator.EQ, genre.getId());
				change(Mode.Artist, filter);
				return null;
			}

			public Track visit(Artist artist)
			{
				Filter filter = new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.ARTIST, Operator.EQ, artist.getId());
				change(Mode.Album, filter);
				return null;
			}

			public Track visit(FSobject FSobject)
			{
				Filter<Tables.Tracks> filter = new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.PATH, Operator.LIKE, FSobject.getFullPath() + "%");
				dirFilter = new FieldFilter<Tables.Dirs, FSobject, String>(Tables.DIRS.PATH, Operator.EQ, FSobject.getFullPath());
				change(Mode.Dir, filter);
				return null;
			}
		});
	}

	/**
	 * @param toMode
	 * @param filter - filter to add, can be null
	 */
	public void change(Mode toMode, final Filter filter)
	{
		if(filter != null)
		{
			filtersAddWithMode.put(currMode, filter);
			filters.add(filter);
			filterList.post(new Runnable()
			{
				public void run()
				{
					filterListAdapter.add(filter);
				}
			});
		}

		currMode = toMode;
		for (final Mode aMode : Mode.values())
		{
			final ImageView button = getButton(aMode);
			button.post(new Runnable()
			{
				public void run()
				{
					button.setImageResource(aMode.equals(currMode) ? aMode.drawableActive : aMode.drawable);
				}
			});
		}
		update();
	}

	public void update()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				switch (currMode)
				{
					case Album:
						List<Instance> albums = new ArrayList<Instance>(database.getAlbumList(getFilter()));
						albums.remove(Album.NO_ALBUM);
						albums.addAll(database.getTrackList(new FilterSet(getFilter(), new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.ALBUM, Operator.EQ, ""))));
						listAdapter.replace(albums);
						break;
					case Artist:
						List<Instance> artists = new ArrayList<Instance>(database.getArtistList(getFilter()));
						artists.remove(Artist.NO_ARTIST);
						artists.addAll(database.getTrackList(new FilterSet(getFilter(), new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.ARTIST, Operator.EQ, ""))));
						listAdapter.replace(artists);
						break;
					case Genre:
						List<Instance> genres = new ArrayList<Instance>(database.getGenreList(getFilter()));
						genres.remove(Album.NO_ALBUM);
						genres.addAll(database.getTrackList(new FilterSet(getFilter(), new FieldFilter<Tables.Tracks, Track, String>(Tables.TRACKS.GENRE, Operator.EQ, ""))));
						listAdapter.replace(genres);
						break;
					case Track:
						listAdapter.replace(database.getTrackList(getFilter()));
						break;
					case Dir:
						listAdapter.replace(database.getDirList(dirFilter));
						break;
					default:
						throw new RuntimeException(currMode.name() + " not expexted here");
				}
			}
		}).start();
	}

	public void updated(final Instance instance)
	{
		if(!Player.Slides.PLAYLIST.equals(listActivity.getCurrSlide()))
		{
			return;
		}

		Instance instanceToAdd = instance.accept(new InstanceVisitor<Instance>()
		{
			public Instance visit(Track track)
			{
				if(getFilter().accept(new MatchFilterVisitor<Track, Tables.Tracks>(track)))
				{
					switch (currMode)
					{
						case Album:
							return track.GetAlbum() == Album.NO_ALBUM ? track : track.GetAlbum();
						case Artist:
							return track.GetArtist() == Artist.NO_ARTIST ? track : track.GetArtist();
						case Genre:
							return track.GetGenre() == Genre.NO_GENRE ? track : track.GetGenre();
						case Track:
							return track;
						case Dir:
							return track;
						default:
							throw new RuntimeException(currMode.name() + " not expexted here");
					}
				}
				return null;
			}

			public Instance visit(TrackDigest track)
			{
				throw new RuntimeException("not supported yet");
			}

			public Instance visit(Album album)
			{
				throw new RuntimeException("not supported yet");
			}

			public Instance visit(Genre genre)
			{
				throw new RuntimeException("not supported yet");
			}

			public Instance visit(Artist artist)
			{
				throw new RuntimeException("not supported yet");
			}

			public Instance visit(FSobject dir)
			{
				if(dirFilter != null)
				{
					return dirFilter.accept(new MatchFilterVisitor<FSobject, Tables.Dirs>(dir)) && Mode.Dir.equals(currMode) ? dir : null;
				}
				return null;
			}
		});

		if(instanceToAdd != null)
		{
			listAdapter.add(instanceToAdd);
		}
	}

	public boolean back(){
		Mode backMode;
		switch (currMode)
		{
			case Album:
				backMode = Mode.Artist;
				break;
			case Artist:
				backMode = Mode.Genre;
				break;
			case Genre:
				backMode = null;
				break;
			case Track:
				backMode = Mode.Album;
				break;
			case Dir:
				backMode = Mode.Dir;
				break;
			default:
				throw new RuntimeException(currMode.name() + " not expexted here");
		}
		final Filter filterAddedByBack = filtersAddWithMode.remove(backMode);
		if(filterAddedByBack == null)
		{
			return true;
		}
		else
		{
			filters.remove(filterAddedByBack);
			filterList.post(new Runnable()
			{
				public void run()
				{
					filterListAdapter.remove(filterAddedByBack);
				}
			});
			change(backMode, null);
			return false;
		}
	}

	public void cleared()
	{
		listAdapter.clear();
	}

	public String getId()
	{
		return "FileChooserUpdater";
	}

	private ImageView getButton(Mode mode)
	{
		return (ImageView) listActivity.findViewById(mode.buttonId);
	}

	protected abstract void filterChoosen(Filter filter);
}
