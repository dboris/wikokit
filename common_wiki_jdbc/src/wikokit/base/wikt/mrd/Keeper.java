/* Keeper.java - manager stores parsed data to MRD Wiktionary database (wikt_parsed).
 * 
 * Copyright (c) 2008-2011 Andrew Krizhanovsky <andrew.krizhanovsky at gmail.com>
 * Distributed under GNU General Public License.
 */

package wikokit.base.wikt.mrd;

import wikokit.base.wikt.word.WPOS;
import wikokit.base.wikt.word.WordBase;
import wikokit.base.wikt.word.WLanguage;
import wikokit.base.wikt.word.WTranslation;
import wikokit.base.wikt.word.WRelation;
import wikokit.base.wikt.word.WMeaning;
import wikokit.base.wikt.constant.Relation;
import wikokit.base.wikt.sql.index.IndexForeign;
import wikokit.base.wikt.sql.index.IndexNative;
import wikokit.base.wikt.sql.TPOS;
import wikokit.base.wikt.sql.TLangPOS;
import wikokit.base.wikt.sql.TTranslation;
import wikokit.base.wikt.sql.TWikiText;
import wikokit.base.wikt.sql.TPage;
import wikokit.base.wikt.sql.TRelation;
import wikokit.base.wikt.sql.TMeaning;
import wikokit.base.wikt.sql.TLang;
import wikokit.base.wikipedia.sql.Connect;
import wikokit.base.wikt.util.WikiText;
import wikokit.base.wikipedia.language.LanguageType;

import java.util.Map;
import wikokit.base.wikt.sql.label.TLabel;
import wikokit.base.wikt.sql.quote.TQuote;

/** Manager stores parsed data to MRD Wiktionary database (wikt_parsed).
 */
public class Keeper {
    // private static boolean DEBUG = true;
    
    /** Stores word data to tables of parsed wiktionary database
     *
     * @param conn connection interface to a parsed wiktionary database
     * @param word data to be stored to a parsed wiktionary database
     * @param native_lang       native language in the Wiktionary,
     *                          e.g. Russian language in Russian Wiktionary
     */
    public static void storeToDB(Connect conn, WordBase word,
                                  LanguageType native_lang) {
        
        String page_title = word.getPageTitle();
        
        // table 'page', stores page title, gets id of new page
        int word_count = 0;
        // to calculate, todo ...
        
        int wiki_link_count = 0;
        // to calculate, todo ...

        boolean is_in_wiktionary = true;
        TPage tpage = TPage.getOrInsert(conn, page_title, word_count, wiki_link_count, 
                is_in_wiktionary, word.getRedirect());

        if(null == tpage) {
            System.out.println("(Keeper.storeToDB()):: TPage.getOrInsert returned null. page_title='" + page_title + "'");
        }

        if(word.isRedirect())
            return;

        boolean b_page_added_to_index_native = false;
        WLanguage[] w_languages = word.getAllLanguages();
        for(WLanguage w_lang : w_languages) {

            LanguageType lang_type = w_lang.getLanguage();
            TLang tlang = TLang.get(lang_type);

            boolean b_native_lang = lang_type == native_lang; // word in native language

            WPOS[] w_pos_all = w_lang.getAllPOS();
            int etymology_n = 0;
            for(WPOS w_pos : w_pos_all) {
                TPOS tpos = TPOS.get(w_pos.getPOS());

                // tpage, tlang, tpos, etymology_n: -> into table 'lang_pos', gets id
                
                String lemma = "";  // todo ...
                TLangPOS lang_pos = TLangPOS.insert(conn, tpage, tlang, tpos, etymology_n, lemma);
                etymology_n ++;

                Map<Relation, WRelation[]> m_relations = w_pos.getAllRelations();
                WTranslation[] translations = w_pos.getAllTranslation();
                WMeaning[] w_meaning_all = w_pos.getAllMeanings();
                for(int i=0; i<w_meaning_all.length; i++) {
                    WMeaning w_meaning = w_meaning_all[i];
                    WikiText definition = w_meaning.getWikiText();
                    TWikiText twiki_text= TWikiText.storeToDB(conn, definition);
                    
                    TMeaning tmeaning = TMeaning.insert(conn, lang_pos, i, twiki_text);

                    TQuote.storeToDB(conn, page_title, tmeaning, tlang, w_meaning.getQuotes());
                    
                    TRelation.storeToDB(conn, tmeaning, i, m_relations);
                    TLabel.storeToDB(conn, page_title, tmeaning, tlang, w_meaning.getLabels());
                    
                    if(translations.length > i) // not every meaning is happy to have it's own translation
                        TTranslation.storeToDB(conn, native_lang, page_title,
                                            lang_pos, tmeaning, translations[i]);
                        
                    twiki_text = null;  // free memory
                    tmeaning = null;
                }

                // some stubs don't have definition, but they have translations
                if(w_meaning_all.length == 0 && translations.length > 0) {
                    for(int i=0; i<translations.length; i++) {
                        TMeaning tmeaning = TMeaning.insert(conn, lang_pos, i, null);
                        TTranslation.storeToDB(conn, native_lang, page_title,
                                            lang_pos, tmeaning, translations[i]);
                        tmeaning = null;    // free memory
                    }
                }

                // index of words
                if(w_meaning_all.length > 0) {
                    if(b_native_lang) { // index of words in native language
                        if(!b_page_added_to_index_native) {
                            b_page_added_to_index_native = true;
                            IndexNative.insert(conn, tpage, !m_relations.isEmpty());
                        }
                    } else
                        IndexForeign.insertIfAbsent(conn, page_title, true,
                                                null, native_lang, lang_type);
                }
                
                tpos = null;            // free memory
                lang_pos = null;
                translations = null;
            }
            tlang = null;
            w_lang = null;
        }
        tpage = null;                   // free memory
        w_languages = null;
        
        // 4. table 'relation', stores relation_id, meaning_id, wiki_text_id,
        // may be: page_id (for simple one-word relation, for relations which are presented in the db)
        //       ? post-processing?

        // 5. table 'translation', stores: translation_id, meaning_summary

        // 6. table 'translation_entry', stores: translation_id, lang_id, wiki_text_id,
        // may be: page_id (for simple one-word translation, for translations which are presented in the db)
        //       ? post-processing?
    }
}
