var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var _WootzappWalletWallet_instances, _WootzappWalletWallet_listeners, _WootzappWalletWallet_version, _WootzappWalletWallet_name, _WootzappWalletWallet_icon, _WootzappWalletWallet_account, _WootzappWalletWallet_wootzappWallet, _WootzappWalletWallet_on, _WootzappWalletWallet_emit, _WootzappWalletWallet_off, _WootzappWalletWallet_connected, _WootzappWalletWallet_disconnected, _WootzappWalletWallet_reconnected, _WootzappWalletWallet_connect, _WootzappWalletWallet_disconnect, _WootzappWalletWallet_signAndSendTransaction, _WootzappWalletWallet_signTransaction, _WootzappWalletWallet_signMessage, _WootzappWalletWallet_signIn;
import { SolanaSignAndSendTransaction, SolanaSignIn, SolanaSignMessage, SolanaSignTransaction, } from '@solana/wallet-standard-features';
import { VersionedTransaction } from '@solana/web3.js';
import { StandardConnect, StandardDisconnect, StandardEvents, } from '@wallet-standard/features';
import bs58 from 'bs58';
import { WootzappWalletWalletAccount } from './account.js';
import { icon } from './icon.js';
import { isSolanaChain, isVersionedTransaction, SOLANA_CHAINS } from './solana.js';
import { bytesEqual } from './util.js';
export const WootzappWalletNamespace = 'wootzappWallet:';
export class WootzappWalletWallet {
    get version() {
        return __classPrivateFieldGet(this, _WootzappWalletWallet_version, "f");
    }
    get name() {
        return __classPrivateFieldGet(this, _WootzappWalletWallet_name, "f");
    }
    get icon() {
        return __classPrivateFieldGet(this, _WootzappWalletWallet_icon, "f");
    }
    get chains() {
        return SOLANA_CHAINS.slice();
    }
    get features() {
        return {
            [StandardConnect]: {
                version: '1.0.0',
                connect: __classPrivateFieldGet(this, _WootzappWalletWallet_connect, "f"),
            },
            [StandardDisconnect]: {
                version: '1.0.0',
                disconnect: __classPrivateFieldGet(this, _WootzappWalletWallet_disconnect, "f"),
            },
            [StandardEvents]: {
                version: '1.0.0',
                on: __classPrivateFieldGet(this, _WootzappWalletWallet_on, "f"),
            },
            [SolanaSignAndSendTransaction]: {
                version: '1.0.0',
                supportedTransactionVersions: ['legacy', 0],
                signAndSendTransaction: __classPrivateFieldGet(this, _WootzappWalletWallet_signAndSendTransaction, "f"),
            },
            [SolanaSignTransaction]: {
                version: '1.0.0',
                supportedTransactionVersions: ['legacy', 0],
                signTransaction: __classPrivateFieldGet(this, _WootzappWalletWallet_signTransaction, "f"),
            },
            [SolanaSignMessage]: {
                version: '1.0.0',
                signMessage: __classPrivateFieldGet(this, _WootzappWalletWallet_signMessage, "f"),
            },
            [SolanaSignIn]: {
                version: '1.0.0',
                signIn: __classPrivateFieldGet(this, _WootzappWalletWallet_signIn, "f"),
            },
            [WootzappWalletNamespace]: {
                wootzappWallet: __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f"),
            },
        };
    }
    get accounts() {
        return __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f") ? [__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f")] : [];
    }
    constructor(wootzappWallet) {
        _WootzappWalletWallet_instances.add(this);
        _WootzappWalletWallet_listeners.set(this, {});
        _WootzappWalletWallet_version.set(this, '1.0.0');
        _WootzappWalletWallet_name.set(this, 'Wootzapp');
        _WootzappWalletWallet_icon.set(this, icon);
        _WootzappWalletWallet_account.set(this, null);
        _WootzappWalletWallet_wootzappWallet.set(this, void 0);
        _WootzappWalletWallet_on.set(this, (event, listener) => {
            __classPrivateFieldGet(this, _WootzappWalletWallet_listeners, "f")[event]?.push(listener) || (__classPrivateFieldGet(this, _WootzappWalletWallet_listeners, "f")[event] = [listener]);
            return () => __classPrivateFieldGet(this, _WootzappWalletWallet_instances, "m", _WootzappWalletWallet_off).call(this, event, listener);
        });
        _WootzappWalletWallet_connected.set(this, () => {
            const address = __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").publicKey?.toBase58();
            if (address) {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                const publicKey = __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").publicKey.toBytes();
                const account = __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f");
                if (!account || account.address !== address || !bytesEqual(account.publicKey, publicKey)) {
                    __classPrivateFieldSet(this, _WootzappWalletWallet_account, new WootzappWalletWalletAccount({ address, publicKey }), "f");
                    __classPrivateFieldGet(this, _WootzappWalletWallet_instances, "m", _WootzappWalletWallet_emit).call(this, 'change', { accounts: this.accounts });
                }
            }
        });
        _WootzappWalletWallet_disconnected.set(this, () => {
            if (__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f")) {
                __classPrivateFieldSet(this, _WootzappWalletWallet_account, null, "f");
                __classPrivateFieldGet(this, _WootzappWalletWallet_instances, "m", _WootzappWalletWallet_emit).call(this, 'change', { accounts: this.accounts });
            }
        });
        _WootzappWalletWallet_reconnected.set(this, () => {
            if (__classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").publicKey) {
                __classPrivateFieldGet(this, _WootzappWalletWallet_connected, "f").call(this);
            }
            else {
                __classPrivateFieldGet(this, _WootzappWalletWallet_disconnected, "f").call(this);
            }
        });
        _WootzappWalletWallet_connect.set(this, async ({ silent } = {}) => {
            if (!__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f")) {
                await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").connect(silent ? { onlyIfTrusted: true } : undefined);
            }
            __classPrivateFieldGet(this, _WootzappWalletWallet_connected, "f").call(this);
            return { accounts: this.accounts };
        });
        _WootzappWalletWallet_disconnect.set(this, async () => {
            await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").disconnect();
        });
        _WootzappWalletWallet_signAndSendTransaction.set(this, async (...inputs) => {
            if (!__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                throw new Error('not connected');
            const outputs = [];
            if (inputs.length === 1) {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                const { transaction, account, chain, options } = inputs[0];
                const { minContextSlot, preflightCommitment, skipPreflight, maxRetries } = options || {};
                if (account !== __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                    throw new Error('invalid account');
                if (!isSolanaChain(chain))
                    throw new Error('invalid chain');
                const { signature } = await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signAndSendTransaction(VersionedTransaction.deserialize(transaction), {
                    preflightCommitment,
                    minContextSlot,
                    maxRetries,
                    skipPreflight,
                });
                outputs.push({ signature: bs58.decode(signature) });
            }
            else if (inputs.length > 1) {
                for (const input of inputs) {
                    outputs.push(...(await __classPrivateFieldGet(this, _WootzappWalletWallet_signAndSendTransaction, "f").call(this, input)));
                }
            }
            return outputs;
        });
        _WootzappWalletWallet_signTransaction.set(this, async (...inputs) => {
            if (!__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                throw new Error('not connected');
            const outputs = [];
            if (inputs.length === 1) {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                const { transaction, account, chain } = inputs[0];
                if (account !== __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                    throw new Error('invalid account');
                if (chain && !isSolanaChain(chain))
                    throw new Error('invalid chain');
                const signedTransaction = await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signTransaction(VersionedTransaction.deserialize(transaction));
                const serializedTransaction = isVersionedTransaction(signedTransaction)
                    ? signedTransaction.serialize()
                    : new Uint8Array(signedTransaction.serialize({
                        requireAllSignatures: false,
                        verifySignatures: false,
                    }));
                outputs.push({ signedTransaction: serializedTransaction });
            }
            else if (inputs.length > 1) {
                let chain = undefined;
                for (const input of inputs) {
                    if (input.account !== __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                        throw new Error('invalid account');
                    if (input.chain) {
                        if (!isSolanaChain(input.chain))
                            throw new Error('invalid chain');
                        if (chain) {
                            if (input.chain !== chain)
                                throw new Error('conflicting chain');
                        }
                        else {
                            chain = input.chain;
                        }
                    }
                }
                const transactions = inputs.map(({ transaction }) => VersionedTransaction.deserialize(transaction));
                const signedTransactions = await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signAllTransactions(transactions);
                outputs.push(...signedTransactions.map((signedTransaction) => {
                    const serializedTransaction = isVersionedTransaction(signedTransaction)
                        ? signedTransaction.serialize()
                        : new Uint8Array(signedTransaction.serialize({
                            requireAllSignatures: false,
                            verifySignatures: false,
                        }));
                    return { signedTransaction: serializedTransaction };
                }));
            }
            return outputs;
        });
        _WootzappWalletWallet_signMessage.set(this, async (...inputs) => {
            if (!__classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                throw new Error('not connected');
            const outputs = [];
            if (inputs.length === 1) {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                const { message, account } = inputs[0];
                if (account !== __classPrivateFieldGet(this, _WootzappWalletWallet_account, "f"))
                    throw new Error('invalid account');
                const { signature } = await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signMessage(message);
                outputs.push({ signedMessage: message, signature });
            }
            else if (inputs.length > 1) {
                for (const input of inputs) {
                    outputs.push(...(await __classPrivateFieldGet(this, _WootzappWalletWallet_signMessage, "f").call(this, input)));
                }
            }
            return outputs;
        });
        _WootzappWalletWallet_signIn.set(this, async (...inputs) => {
            const outputs = [];
            if (inputs.length > 1) {
                for (const input of inputs) {
                    outputs.push(await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signIn(input));
                }
            }
            else {
                return [await __classPrivateFieldGet(this, _WootzappWalletWallet_wootzappWallet, "f").signIn(inputs[0])];
            }
            return outputs;
        });
        if (new.target === WootzappWalletWallet) {
            Object.freeze(this);
        }
        __classPrivateFieldSet(this, _WootzappWalletWallet_wootzappWallet, wootzappWallet, "f");
        wootzappWallet.on('connect', __classPrivateFieldGet(this, _WootzappWalletWallet_connected, "f"), this);
        wootzappWallet.on('disconnect', __classPrivateFieldGet(this, _WootzappWalletWallet_disconnected, "f"), this);
        wootzappWallet.on('accountChanged', __classPrivateFieldGet(this, _WootzappWalletWallet_reconnected, "f"), this);
        __classPrivateFieldGet(this, _WootzappWalletWallet_connected, "f").call(this);
    }
}
_WootzappWalletWallet_listeners = new WeakMap(), _WootzappWalletWallet_version = new WeakMap(), _WootzappWalletWallet_name = new WeakMap(), _WootzappWalletWallet_icon = new WeakMap(), _WootzappWalletWallet_account = new WeakMap(), _WootzappWalletWallet_wootzappWallet = new WeakMap(), _WootzappWalletWallet_on = new WeakMap(), _WootzappWalletWallet_connected = new WeakMap(), _WootzappWalletWallet_disconnected = new WeakMap(), _WootzappWalletWallet_reconnected = new WeakMap(), _WootzappWalletWallet_connect = new WeakMap(), _WootzappWalletWallet_disconnect = new WeakMap(), _WootzappWalletWallet_signAndSendTransaction = new WeakMap(), _WootzappWalletWallet_signTransaction = new WeakMap(), _WootzappWalletWallet_signMessage = new WeakMap(), _WootzappWalletWallet_signIn = new WeakMap(), _WootzappWalletWallet_instances = new WeakSet(), _WootzappWalletWallet_emit = function _WootzappWalletWallet_emit(event, ...args) {
    // eslint-disable-next-line prefer-spread
    __classPrivateFieldGet(this, _WootzappWalletWallet_listeners, "f")[event]?.forEach((listener) => listener.apply(null, args));
}, _WootzappWalletWallet_off = function _WootzappWalletWallet_off(event, listener) {
    __classPrivateFieldGet(this, _WootzappWalletWallet_listeners, "f")[event] = __classPrivateFieldGet(this, _WootzappWalletWallet_listeners, "f")[event]?.filter((existingListener) => listener !== existingListener);
};
//# sourceMappingURL=wallet.js.map