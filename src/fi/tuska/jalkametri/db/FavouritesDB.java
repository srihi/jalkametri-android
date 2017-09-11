/**
 * Copyright 2006-2011 Tuukka Haapasalo
 * 
 * This file is part of jAlkaMetri.
 * 
 * jAlkaMetri is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * jAlkaMetri is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with jAlkaMetri (LICENSE.txt). If not, see <http://www.gnu.org/licenses/>.
 */
package fi.tuska.jalkametri.db;

import static fi.tuska.jalkametri.db.DBAdapter.KEY_COMMENT;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_ICON;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_ID;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_NAME;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_ORDER;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_SIZE_NAME;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_STRENGTH;
import static fi.tuska.jalkametri.db.DBAdapter.KEY_VOLUME;
import static fi.tuska.jalkametri.db.DBAdapter.TAG;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import fi.tuska.jalkametri.dao.Favourites;
import fi.tuska.jalkametri.data.Drink;
import fi.tuska.jalkametri.data.DrinkEvent;
import fi.tuska.jalkametri.data.DrinkSelection;
import fi.tuska.jalkametri.data.DrinkSize;
import fi.tuska.jalkametri.util.LogUtil;
import fi.tuska.jalkametri.util.TimeUtil;

public class FavouritesDB extends AbstractDB implements Favourites {

    public static final String TABLE_NAME = "favourites";

    public static final String SQL_CREATE_TABLE_FAVOURITES_1 = "CREATE TABLE " + TABLE_NAME
        + " (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "name TEXT NOT NULL, "
        + "strength FLOAT NOT NULL, " + "volume FLOAT NOT NULL, " + "size_name TEXT NOT NULL, "
        + "icon TEXT NOT NULL, " + "pos INTEGER NOT NULL);";

    public static final String SQL_CREATE_TABLE_FAVOURITES_2 = "CREATE TABLE " + TABLE_NAME
        + " (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "name TEXT NOT NULL, "
        + "strength FLOAT NOT NULL, " + "volume FLOAT NOT NULL, " + "size_name TEXT NOT NULL, "
        + "icon TEXT NOT NULL, " + "comment TEXT NOT NULL DEFAULT '', "
        + "pos INTEGER NOT NULL);";

    private static final String[] FAVOURITE_QUERY_COLUMNS = new String[] { KEY_ID, KEY_NAME,
        KEY_STRENGTH, KEY_VOLUME, KEY_SIZE_NAME, KEY_ICON, KEY_COMMENT, KEY_ORDER };

    private final TimeUtil timeUtil;
    
    public FavouritesDB(DBAdapter adapter, Context context) {
        super(adapter, TABLE_NAME);
        this.timeUtil = new TimeUtil(context);
    }

    @Override
    public void createFavourite(DrinkSelection fav) {
        int maxOrder = getLargestOrderNumber();
        ContentValues values = new ContentValues();
        DrinkSelectionHelper.createCommonValues(values, fav);
        values.put(KEY_ORDER, maxOrder + 1);

        long id = adapter.getDatabase().insert(TABLE_NAME, null, values);
        assert id >= 0;
    }

    @Override
    public boolean deleteFavourite(long index) {
        DBDataObject.enforceBackedObject(index);

        int deleted = adapter.getDatabase().delete(TABLE_NAME, getIndexClause(index), null);
        return deleted > 0;
    }

    @Override
    public List<DrinkEvent> getFavourites() {
        return getFavourites(0);
    }

    @Override
    public List<DrinkEvent> getFavourites(int limit) {
        LogUtil.d(TAG, "Querying for favourites");

        Cursor cursor = adapter.getDatabase().query(false, TABLE_NAME, FAVOURITE_QUERY_COLUMNS,
            null, null, null, null, KEY_ORDER, limit > 0 ? String.valueOf(limit) : null);
        int count = cursor.getCount();
        List<DrinkEvent> drinks = new ArrayList<DrinkEvent>(count);
        if (cursor.moveToFirst()) {
            do {
                int c = -1;
                drinks.add(createDrinkSelection(cursor.getLong(++c), cursor.getString(++c),
                    cursor.getDouble(++c), cursor.getDouble(++c), cursor.getString(++c),
                    cursor.getString(++c), cursor.getString(++c)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return drinks;
    }

    private DrinkEvent createDrinkSelection(long eventId, String name, double strength,
        double volume, String sizeName, String icon, String comment) {
        DBDataObject.enforceBackedObject(eventId);

        Drink drink = new Drink(name, strength, icon, comment, new ArrayList<DrinkSize>());
        DrinkSize size = new DrinkSize(sizeName, volume, icon);
        return new DrinkEvent(eventId, drink, size, timeUtil.getCurrentTime());
    }

    @Override
    public boolean updateFavourite(long index, DrinkEvent fav) {
        DBDataObject.enforceBackedObject(index);

        ContentValues newValues = new ContentValues();
        DrinkSelectionHelper.createCommonValues(newValues, fav);
        int updated = adapter.getDatabase().update(TABLE_NAME, newValues, getIndexClause(index),
            null);
        return updated > 0;
    }

    @Override
    public DrinkEvent getFavourite(long index) {
        DBDataObject.enforceBackedObject(index);
        LogUtil.d(TAG, "Querying for favourite %d", index);

        Cursor cursor = adapter.getDatabase().query(true, TABLE_NAME, FAVOURITE_QUERY_COLUMNS,
            getIndexClause(index), null, null, null, null, null);
        DrinkEvent event = null;
        if (cursor.moveToFirst()) {
            int c = -1;
            event = createDrinkSelection(cursor.getLong(++c), cursor.getString(++c),
                cursor.getDouble(++c), cursor.getDouble(++c), cursor.getString(++c),
                cursor.getString(++c), cursor.getString(++c));
        }
        cursor.close();

        return event;

    }

}