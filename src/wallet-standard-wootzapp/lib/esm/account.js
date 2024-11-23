// This is copied with modification from @wallet-standard/wallet
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
var _WootzappWalletWalletAccount_address, _WootzappWalletWalletAccount_publicKey, _WootzappWalletWalletAccount_chains, _WootzappWalletWalletAccount_features, _WootzappWalletWalletAccount_label, _WootzappWalletWalletAccount_icon;
import { SolanaSignAndSendTransaction, SolanaSignMessage, SolanaSignTransaction, } from '@solana/wallet-standard-features';
import { SOLANA_CHAINS } from './solana.js';
const chains = SOLANA_CHAINS;
const features = [SolanaSignAndSendTransaction, SolanaSignTransaction, SolanaSignMessage];
export class WootzappWalletWalletAccount {
    get address() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_address, "f");
    }
    get publicKey() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_publicKey, "f").slice();
    }
    get chains() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_chains, "f").slice();
    }
    get features() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_features, "f").slice();
    }
    get label() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_label, "f");
    }
    get icon() {
        return __classPrivateFieldGet(this, _WootzappWalletWalletAccount_icon, "f");
    }
    constructor({ address, publicKey, label, icon }) {
        _WootzappWalletWalletAccount_address.set(this, void 0);
        _WootzappWalletWalletAccount_publicKey.set(this, void 0);
        _WootzappWalletWalletAccount_chains.set(this, void 0);
        _WootzappWalletWalletAccount_features.set(this, void 0);
        _WootzappWalletWalletAccount_label.set(this, void 0);
        _WootzappWalletWalletAccount_icon.set(this, void 0);
        if (new.target === WootzappWalletWalletAccount) {
            Object.freeze(this);
        }
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_address, address, "f");
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_publicKey, publicKey, "f");
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_chains, chains, "f");
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_features, features, "f");
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_label, label, "f");
        __classPrivateFieldSet(this, _WootzappWalletWalletAccount_icon, icon, "f");
    }
}
_WootzappWalletWalletAccount_address = new WeakMap(), _WootzappWalletWalletAccount_publicKey = new WeakMap(), _WootzappWalletWalletAccount_chains = new WeakMap(), _WootzappWalletWalletAccount_features = new WeakMap(), _WootzappWalletWalletAccount_label = new WeakMap(), _WootzappWalletWalletAccount_icon = new WeakMap();
//# sourceMappingURL=account.js.map