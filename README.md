# Rakenduse kirjeldus


Tegemist on käsurea rakendusega. Rakendust kävitades tuleb määrata sellele valiidne port. Kasutusel oleva ning nonInt sisendi sisestades antakse võimalus uus valik teha.

**Naabrite leidmine**

Iga klient pärib iga 60 sekundi tagant keskselt serverilt andmeid,
server ise pingib kõiki olemasolevaid aadressi iga 5 sekundi tagant, et veenduda nende valiidsuses.

Seda on võimalik ka manuaalselt klienti poolt jooksutada kasutades commandi update.


**Faili pärimine**


Kui mingi klient soovib alla laadida faili, siis tuleb käsureale kirjutada käsk "*download http://mingifail.com/test.jpg*", kus allalaetava faili url on kasutaja poolt määratav. 

Teatud aja möödudes tuleb, kui klient on faili kätte saanud tuleb käsureale teavitus *FILE RECEIVED*, siis fail salvestatakse **/downloaded-files** kausta.

Võib juhtuda, pärast pikka aega pole faili ikkagi alla laetud, siis tuleb uuesti proovida.

Lisaks sellele on käskulsed current, stop ja help.
Current näitab praeguseid naabreid.
Stop peatab klienti.
Help selgitab kõiki käsklusi.
