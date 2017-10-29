package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import eu.kotrzena.peasantconquest.game.PlayerInfo;

public class PlayerListAdapter extends ArrayAdapter<PlayerInfo> {
	private static class SparsearrayList<T> implements List<T> {
		SparseArray<T> list;
		SparsearrayList(SparseArray<T> list){
			this.list = list;
		}

		@Override
		public int size() {
			if(list == null)
				return 0;
			else
				return list.size();
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}

		@Override
		public boolean contains(Object o) {
			return list.indexOfValue((T)o) >= 0;
		}

		@NonNull
		@Override
		public Iterator<T> iterator() {
			return null;
		}

		@NonNull
		@Override
		public Object[] toArray() {
			return new Object[0];
		}

		@NonNull
		@Override
		public <T1> T1[] toArray(@NonNull T1[] t1s) {
			return null;
		}

		@Override
		public boolean add(T t) {
			return false;
		}

		@Override
		public boolean remove(Object o) {
			return false;
		}

		@Override
		public boolean containsAll(@NonNull Collection<?> collection) {
			return false;
		}

		@Override
		public boolean addAll(@NonNull Collection<? extends T> collection) {
			return false;
		}

		@Override
		public boolean addAll(int i, @NonNull Collection<? extends T> collection) {
			return false;
		}

		@Override
		public boolean removeAll(@NonNull Collection<?> collection) {
			return false;
		}

		@Override
		public boolean retainAll(@NonNull Collection<?> collection) {
			return false;
		}

		@Override
		public void clear() {

		}

		@Override
		public T get(int i) {
			return list.valueAt(i);
		}

		@Override
		public T set(int i, T t) {
			return null;
		}

		@Override
		public void add(int i, T t) {

		}

		@Override
		public T remove(int i) {
			return null;
		}

		@Override
		public int indexOf(Object o) {
			return list.indexOfValue((T)o);
		}

		@Override
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}

		@Override
		public ListIterator<T> listIterator() {
			return null;
		}

		@NonNull
		@Override
		public ListIterator<T> listIterator(int i) {
			return null;
		}

		@NonNull
		@Override
		public List<T> subList(int i, int i1) {
			return null;
		}
	}

	SparseArray<PlayerInfo> list;
	private GameActivity activity;

	public PlayerListAdapter(@NonNull GameActivity context, SparseArray<PlayerInfo> list) {
		super(context, R.layout.player_list_item_layout, new SparsearrayList<PlayerInfo>(list));
		this.list = list;
		this.activity = context;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		View row = convertView;

		final PlayerInfo player = list.valueAt(position);
		if(row == null)	{
			LayoutInflater inflater = activity.getLayoutInflater();
			row = inflater.inflate(R.layout.player_list_item_layout, parent, false);
			final CheckBox readyBox = row.findViewById(R.id.checkBoxReady);
			if(activity.game.getCurrentPlayerId() == player.id)
				readyBox.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						activity.ready(((CheckBox)view).isChecked());
					}
				});
			else
				readyBox.setEnabled(false);
		}

		((ImageView) row.findViewById(R.id.imageViewColor)).setColorFilter(player.color);
		if(player.playerName.isEmpty()){
			((TextView) row.findViewById(R.id.textPlayer)).setText("");
			((TextView) row.findViewById(R.id.textDesc)).setText(R.string.empty_slot);
			((CheckBox) row.findViewById(R.id.checkBoxReady)).setChecked(player.ready);
		} else {
			((TextView) row.findViewById(R.id.textPlayer)).setText(player.playerName);
			if (player.isHost)
				((TextView) row.findViewById(R.id.textDesc)).setText(R.string.host);
			else
				((TextView) row.findViewById(R.id.textDesc)).setText("");
			((CheckBox) row.findViewById(R.id.checkBoxReady)).setChecked(player.ready);
		}
		return row;
	}
}
