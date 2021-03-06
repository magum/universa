/*
 * Copyright (c) 2017 Sergey Chernov, iCodici S.n.C, All Rights Reserved
 *
 * Written by Sergey Chernov <real.sergeych@gmail.com>, August 2017.
 *
 */

package com.icodici.universa.contract;

import com.icodici.crypto.PublicKey;
import com.icodici.universa.Errors;
import net.sergeych.diff.ChangedItem;
import net.sergeych.diff.Delta;
import net.sergeych.diff.MapDelta;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static com.icodici.universa.Errors.*;

public class ContractDelta {

    private final Contract existing;
    private final Contract changed;
    private MapDelta stateDelta;
    private Map<String,Delta> stateChanges;
    private Role creator;

    public ContractDelta(Contract existing, Contract changed) {
        this.existing = existing;
        this.changed = changed;
    }

    public void check() {
        try {
            MapDelta rootDelta = Delta.between(existing.serializeToBinder(), changed.serializeToBinder());
            MapDelta definitionDelta = (MapDelta) rootDelta.getChange("definition");
            if (definitionDelta != null) {
                addError(ILLEGAL_CHANGE, "definition", "definition must not be changed");
            }
            stateDelta = (MapDelta) rootDelta.getChange("state");
            if (rootDelta.isEmpty()) {
                addError(BADSTATE, "", "new state is identical");
            }
            // check immutable root area
            // should be only one change here: state
            if( rootDelta.getChanges().size() > 1)
                addError(ILLEGAL_CHANGE, "roo", "root level changes are forbidden except the state");
            // check immutanle definition arer
            checkStateChange();
            // check only permitted changes in data
        }
        catch(ClassCastException e) {
            e.printStackTrace();
            addError(FAILED_CHECK, "", "failed to compare, structure is broken or not supported");
        }
    }

    private void checkStateChange() {
        stateChanges = stateDelta.getChanges();
        stateChanges.remove("created_by");
        creator = changed.getRole("creator");
        if( creator == null ) {
            addError(MISSING_CREATOR, "state.created_by", "");
            return;
        }
        ChangedItem<Integer,Integer> revision = (ChangedItem) stateChanges.get("revision");
        if( revision == null )
            addError(BAD_VALUE, "state,revision", "is not incremented");
        else {
            stateChanges.remove("revision");
            if( revision.oldValue() + 1 != revision.newValue() )
                addError(BAD_VALUE, "state.revision", "wrong revision number");
        }
        Delta creationTimeChange = stateChanges.get("created_at");
        if( creationTimeChange == null || !(creationTimeChange instanceof ChangedItem) )
            addError(BAD_VALUE, "stat.created_at", "invlaid new state");
        else {
            stateChanges.remove("created_at");
            ChangedItem<LocalDateTime,LocalDateTime> ci = (ChangedItem)creationTimeChange;
            if( !ci.newValue().isAfter(ci.oldValue()) )
                addError(BAD_VALUE, "state.created_at", "new creation datetime is before old one");
        }

        excludePermittedChanges();

        if( !stateChanges.isEmpty() ) {
            addError(FORBIDDEN, "state", "not permitted changes: "+
                    stateChanges.keySet());
        }
    }

    private void excludePermittedChanges() {
        Set<PublicKey> creatorKeys = creator.getKeys();
        existing.getPermissions().values().forEach(permission->{
            if( permission.isAllowedForKeys(creatorKeys))
                permission.checkChanges(existing, stateChanges);
        });
    }

    private void checkOwnerChanged() {
        ChangedItem<Role, Role> oc = (ChangedItem<Role, Role>) stateChanges.get("owner");
        if( oc != null ) {
            stateChanges.remove("owner");
            if( !existing.isPermitted("change_owner", creator) )
                addError(FORBIDDEN, "state.owner", "creator has no right to change");
        }
    }

    private void addError(Errors code, String field, String text) {
        changed.addError(code, field, text);
    }
}
