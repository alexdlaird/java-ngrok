package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

/**
 * An object that represents header configuration for a {@link com.github.alexdlaird.ngrok.protocol.CreateTunnel}.
 */
public class TunnelHeader {
    private final List<String> add;

    private final List<String> remove;

    private TunnelHeader(final TunnelHeader.Builder builder) {
        this.add = builder.add;
        this.remove = builder.remove;
    }

    /**
     * Get the list of headers to add.
     */
    public List<String> getAdd() {
        return add;
    }

    /**
     * Get the list of headers to remove.
     */
    public List<String> getRemove() {
        return remove;
    }

    public static class Builder {
        private List<String> add;
        private List<String> remove;

        public Builder() {
        }

        /**
         * The list of headers to add.
         */
        public TunnelHeader.Builder withAdd(final List<String> add) {
            this.add = add;
            return this;
        }

        /**
         * The list of headers to remove.
         */
        public TunnelHeader.Builder withRemove(final List<String> remove) {
            this.remove = remove;
            return this;
        }

        public TunnelHeader build() {
            return new TunnelHeader(this);
        }
    }
}
