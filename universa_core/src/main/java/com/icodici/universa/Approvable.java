/*
 * Copyright (c) 2017 Sergey Chernov, iCodici S.n.C, All Rights Reserved
 *
 * Written by Sergey Chernov <real.sergeych@gmail.com>, August 2017.
 *
 */

package com.icodici.universa;

import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface to anything that could be approved by the Universa network. Any entity, not limiting to the SmartContract,
 * could be used.
 * <p>
 * Approvable logic:
 * <p>
 * Approvable instance provides self {@link #check()}, and list of other items. Note that transaction that have no
 * output, e.g. empty {@link #getRevokingItems()} and {@link #getNewItems()} will not change the ledger and therefore
 * will not be processed by the network.
 * <p>
 * Created by sergeych on 16/07/2017.
 */
public interface Approvable extends HashIdentifiable {

    /**
     * List of items (e.g. smartcontracts) that should be valid until the end of the operation. These items will be
     * write-locked until the consensus is found.
     *
     * @return referenced items list
     */
    default Set<HashId> getReferencedItems() {
        return new HashSet<>();
    }

    /**
     * List of items that will be revoked on positive consensus. These items will be exclusively locked until the
     * transaction is done. On success (positive consensus) these items will be revoked!
     *
     * @return list of items revoked by this transaction.
     */
    default Set<Approvable> getRevokingItems() {
        return new HashSet<>();
    }

    /**
     * get items created by this transaction. Note, that if the Approvable items does not add self to this list, it will
     * not receive approved state on the positive consensus. Thus, the transactional Apptovable instance could create
     * self, or create other instances on success.
     *
     * @return list of items to approve.
     */
    default Set<? extends Approvable> getNewItems() {
        return new HashSet<Approvable>();
    }

    /**
     * Check the the document is valid assuming all mentioned items are OK, e.g. items, approved by it (if any),
     * items revoking by it and referenced  by it.
     *
     * @return true if this instance is completely checked with positive result.
     */
    boolean check();

    default void addError(ErrorRecord r) {
    }

    default void addError(Errors code,String object,String message) {
        addError(new ErrorRecord(code, object, message));
    }

    default Collection<ErrorRecord> getErrors() {
        return Collections.emptyList();
    }
}
