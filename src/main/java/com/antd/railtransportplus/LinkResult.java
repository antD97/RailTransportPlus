package com.antd.railtransportplus;

public enum LinkResult {
    SAME_CART("Link failed: same cart"),
    CART_REMOVED("Link failed: cart missing"),
    HAS_PREV_CART("Link failed: cart is already linked"),
    LINKED_CART_NOT_FRONT("Link failed: linked cart must be the front of its train"),
    SAME_TRAIN("Link failed: cart already connected to train"),
    FURNACE_HEAD_ONLY("Link failed: furnace minecarts at front of train only"),
    FURNACE_CART_LIMIT("Link failed: furnace minecart limit"),
    CART_LIMIT("Link failed: cart limit"),
    SUCCESS("Linked!");

    public final String message;

    LinkResult(String message) {
        this.message = message;
    }
}
