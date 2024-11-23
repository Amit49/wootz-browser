import { type SolanaSignAndSendTransactionFeature, type SolanaSignInFeature, type SolanaSignMessageFeature, type SolanaSignTransactionFeature } from '@solana/wallet-standard-features';
import type { Wallet } from '@wallet-standard/base';
import { type StandardConnectFeature, type StandardDisconnectFeature, type StandardEventsFeature } from '@wallet-standard/features';
import { WootzappWalletWalletAccount } from './account.js';
import type { WootzappWallet } from './window.js';
export declare const WootzappWalletNamespace = "wootzappWallet:";
export type WootzappWalletFeature = {
    [WootzappWalletNamespace]: {
        wootzappWallet: WootzappWallet;
    };
};
export declare class WootzappWalletWallet implements Wallet {
    #private;
    get version(): "1.0.0";
    get name(): "Wootzapp";
    get icon(): `data:image/svg+xml;base64,${string}` | `data:image/webp;base64,${string}` | `data:image/png;base64,${string}` | `data:image/gif;base64,${string}`;
    get chains(): ("solana:mainnet" | "solana:devnet" | "solana:testnet" | "solana:localnet")[];
    get features(): StandardConnectFeature & StandardDisconnectFeature & StandardEventsFeature & SolanaSignAndSendTransactionFeature & SolanaSignTransactionFeature & SolanaSignMessageFeature & SolanaSignInFeature & WootzappWalletFeature;
    get accounts(): WootzappWalletWalletAccount[];
    constructor(wootzappWallet: WootzappWallet);
}
//# sourceMappingURL=wallet.d.ts.map